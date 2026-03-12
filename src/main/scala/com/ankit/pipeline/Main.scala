package com.ankit.pipeline

import cats.effect.*
import doobie.*
import doobie.hikari.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import com.comcast.ip4s.*
import com.ankit.pipeline.kafka.LogKafkaConsumer
import com.ankit.pipeline.pipeline.Parser
import com.ankit.pipeline.db.MetricsRepo
import com.ankit.pipeline.api.HttpApi
import cats.data.Validated.*

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

      // HTTP Server
      val serverResource = EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8081")
        .withHttpApp(Router("/" -> HttpApi.routes).orNotFound)
        .build

      // Kafka Stream
      val kafkaStream =
        LogKafkaConsumer
          .stream("172.25.190.6:9092", "app-logs")
          .map(Parser.parse)
          .evalMap {
            case Valid(entry) =>
              MetricsRepo.insert(entry) *>
              IO.println(s"✅ Saved [${entry.level}] ${entry.service} — ${entry.message}")
            case Invalid(errs) =>
              IO.println(s"❌ Parse error: ${errs.toList.mkString(", ")}")
          }
          .compile.drain

      // Dono saath chalao
      serverResource.use { server =>
        IO.println(s"=== API Server started at http://localhost:8081 ===") *>
        IO.println("=== Kafka Pipeline Starting ===") *>
        kafkaStream
      }
    }
