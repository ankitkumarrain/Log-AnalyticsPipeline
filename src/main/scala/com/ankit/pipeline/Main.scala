package com.ankit.pipeline

import cats.effect.*
import cats.effect.std.Queue
import doobie.*
import doobie.hikari.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import com.comcast.ip4s.*
import com.ankit.pipeline.kafka.LogKafkaConsumer
import com.ankit.pipeline.pipeline.{Parser, Aggregator}
import com.ankit.pipeline.db.MetricsRepo
import com.ankit.pipeline.api.HttpApi
import cats.data.Validated.*
import scala.concurrent.duration.*
import fs2.Stream

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
    transactor.use { xa =>
      given Transactor[IO] = xa

      val serverResource = EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8081")
        .withHttpApp(Router("/" -> HttpApi.routes).orNotFound)
        .build

      // Ek hi stream — dono kaam karo
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
          .evalMap { metrics =>
            MetricsRepo.insertWindowMetrics(metrics) *>
            IO.println(s"📊 Window [${metrics.level}] ${metrics.service} — count: ${metrics.count}")
          }

      serverResource.use { _ =>
        IO.println("=== API Server started at http://localhost:8081 ===") *>
        IO.println("=== Kafka Pipeline Starting ===") *>
        pipeline.compile.drain
      }
    }
