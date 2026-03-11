package com.ankit.pipeline

import cats.effect.*
import fs2.*
import com.ankit.pipeline.kafka.LogKafkaConsumer
import com.ankit.pipeline.pipeline.Parser
import cats.data.Validated.*

object Main extends IOApp.Simple:
  def run: IO[Unit] =
    IO.println("=== Kafka Pipeline Starting ===") *>
    LogKafkaConsumer
      .stream("localhost:9092", "app-logs")
      .map(Parser.parse)
      .evalMap {
        case Valid(entry) =>
          IO.println(s"✅ [${entry.level}] ${entry.service} — ${entry.message}")
        case Invalid(errs) =>
          IO.println(s"❌ Errors: ${errs.toList.mkString(", ")}")
      }
      .compile.drain
