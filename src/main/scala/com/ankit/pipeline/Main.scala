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
import cats.data.Validated.*
import scala.concurrent.duration.*

object Main extends IOApp.Simple:

  def transactor: Resource[IO, Transactor[IO]] =
    HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5432/logs_db",
      "admin",
      "secret",
      scala.concurrent.ExecutionContext.global
    )

  def run: IO[Unit] =
    val appResource = for
      xa     <- transactor
      client <- EmberClientBuilder.default[IO].build
      server <- EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8081")
        .withHttpApp(Router("/" -> HttpApi.routes(using xa)).orNotFound)
        .build
    yield (xa, client)

    appResource.use { (xa, client) =>
      given Transactor[IO] = xa

      val pipeline =
        LogKafkaConsumer
          .stream("172.25.190.6:9092", "app-logs")
          .map(Parser.parse)
          .collect { case Valid(entry) => entry }
          .evalMap { entry =>
            MetricsRepo.insert(entry) *>
            IO.println(s"✅ Saved [${entry.level}] ${entry.service} — ${entry.message}")
              .as(entry)
          }
          .through(Aggregator.windowPipe(30.seconds))
          .through(Aggregator.criticalOnly)
          .evalTap { metrics =>
            MetricsRepo.insertWindowMetrics(metrics) *>
            IO.println(s"📊 Window [${metrics.level}] ${metrics.service} — count: ${metrics.count}")
          }
          .through(AlertEngine.alertPipe(client))

      IO.println("=== API Server started at http://localhost:8081 ===") *>
      IO.println("=== Kafka Pipeline Starting ===") *>
      pipeline.compile.drain
    }
