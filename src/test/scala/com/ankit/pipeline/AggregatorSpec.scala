package com.ankit.pipeline

import munit.CatsEffectSuite
import cats.effect.IO
import fs2.Stream
import com.ankit.pipeline.pipeline.{Aggregator, WindowMetrics}
import com.ankit.pipeline.domain.*
import java.time.Instant
import scala.concurrent.duration.*

class AggregatorSpec extends CatsEffectSuite:

  def makeEntry(level: LogLevel, service: String): LogEntry =
    LogEntry(Instant.now(), level, service, "test message", None)

  test("ERROR entries window mein aggregate hone chahiye"):
    val entries = List(
      makeEntry(LogLevel.ERROR, "payment-service"),
      makeEntry(LogLevel.ERROR, "payment-service"),
      makeEntry(LogLevel.ERROR, "payment-service"),
    )

    Stream
      .emits(entries)
      .covary[IO]
      .through(Aggregator.windowPipe(100.milliseconds))
      .compile
      .toList
      .map { metrics =>
        val errorMetrics = metrics.filter(_.level == LogLevel.ERROR)
        assert(errorMetrics.nonEmpty)
        assert(errorMetrics.exists(_.service == "payment-service"))
      }

  test("criticalOnly sirf ERROR aur WARN rakhna chahiye"):
    val metrics = List(
      WindowMetrics("svc", LogLevel.ERROR, 3, "30s"),
      WindowMetrics("svc", LogLevel.INFO, 5, "30s"),
      WindowMetrics("svc", LogLevel.WARN, 2, "30s"),
      WindowMetrics("svc", LogLevel.DEBUG, 1, "30s"),
    )

    Stream
      .emits(metrics)
      .covary[IO]
      .through(Aggregator.criticalOnly)
      .compile
      .toList
      .map { result =>
        assertEquals(result.length, 2)
        assert(result.forall(m => m.level == LogLevel.ERROR || m.level == LogLevel.WARN))
      }
