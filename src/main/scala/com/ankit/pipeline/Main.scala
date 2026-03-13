package com.ankit.pipeline

import cats.effect.*
import doobie.*
import doobie.hikari.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.server.Router
import com.comcast.ip4s.*
import com.ankit.pipeline.kafka.LogKafkaConsumer
import com.ankit.pipeline.pipeline.{Parser, Aggregator, AlertEngine}
import com.ankit.pipeline.db.MetricsRepo
import com.ankit.pipeline.api.HttpApi
import com.ankit.pipeline.config.AppConfig
import cats.data.Validated.*
import scala.concurrent.duration.*

object Main extends IOApp.Simple:

  def transactor(config: com.ankit.pipeline.config.DatabaseConfig): Resource[IO, Transactor[IO]] =
    HikariTransactor.newHikariTransactor[IO](
      config.driver,
      config.url,
      config.user,
      config.password,
      scala.concurrent.ExecutionContext.global
    )

  def run: IO[Unit] =
    AppConfig.load match
      case Left(err) =>
        IO.println(s"Config error: $err")

      case Right(config) =>
        val appResource = for
          xa     <- transactor(config.database)
          client <- EmberClientBuilder.default[IO].build
          server <- EmberServerBuilder
            .default[IO]
            .withHost(Host.fromString(config.server.host).get)
            .withPort(Port.fromInt(config.server.port).get)
            .withHttpApp(Router("/" -> HttpApi.routes(using xa)).orNotFound)
            .build
        yield (xa, client)

        appResource.use { (xa, client) =>
          given Transactor[IO] = xa

          val pipeline =
            LogKafkaConsumer
              .stream(config.kafka.bootstrapServers, config.kafka.topic)
              .map(Parser.parse)
              .collect { case Valid(entry) => entry }
              .evalMap { entry =>
                MetricsRepo.insert(entry) *>
                IO.println(s"✅ Saved [${entry.level}] ${entry.service} — ${entry.message}")
                  .as(entry)
              }
              .through(Aggregator.windowPipe(config.pipeline.windowSeconds.seconds))
              .through(Aggregator.criticalOnly)
              .evalTap { metrics =>
                MetricsRepo.insertWindowMetrics(metrics) *>
                IO.println(s"📊 Window [${metrics.level}] ${metrics.service} — count: ${metrics.count}")
              }
              .through(AlertEngine.alertPipe(client))

          IO.println(s"=== API Server started at http://${config.server.host}:${config.server.port} ===") *>
          IO.println("=== Kafka Pipeline Starting ===") *>
          pipeline.compile.drain
        }
