package com.ankit.pipeline

import cats.effect.*
import com.ankit.pipeline.pipeline.Parser
import cats.data.Validated.*

object Main extends IOApp.Simple:
  def run: IO[Unit] =
    val validLog   = "2024-01-15T10:30:00Z|ERROR|payment-service|Connection timeout|trace-123"
    val invalidLog = "bad-timestamp|UNKNOWN|  |  "

    val result1 = Parser.parse(validLog)
    val result2 = Parser.parse(invalidLog)

    IO.println("=== Parser Test ===") *>
    IO.println(s"Valid log result:   $result1") *>
    IO.println(s"Invalid log result: $result2")
