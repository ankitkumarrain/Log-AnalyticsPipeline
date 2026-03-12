package com.ankit.pipeline.pipeline

import cats.effect.*
import fs2.*
import fs2.kafka.*
import com.ankit.pipeline.domain.*
import scala.concurrent.duration.*

case class WindowMetrics(
  service: String,
  level: LogLevel,
  count: Long,
  windowDuration: String
)

object Aggregator:

  // 1 minute window mein entries group karo
  def windowPipe(windowSize: FiniteDuration): Pipe[IO, LogEntry, WindowMetrics] =
    stream =>
      stream
        .groupWithin(1000, windowSize)
        .map { chunk =>
          val entries = chunk.toList

          // Service + Level ke hisaab se group karo
          entries
            .groupBy(e => (e.service, e.level))
            .map { case ((service, level), logs) =>
              WindowMetrics(
                service = service,
                level = level,
                count = logs.size.toLong,
                windowDuration = windowSize.toString
              )
            }
            .toList
        }
        .flatMap(metrics => Stream.emits(metrics))

  // Sirf ERROR aur WARN filter karo
  val criticalOnly: Pipe[IO, WindowMetrics, WindowMetrics] =
    _.filter(m => m.level == LogLevel.ERROR || m.level == LogLevel.WARN)
