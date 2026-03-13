package com.ankit.pipeline

import munit.CatsEffectSuite
import com.ankit.pipeline.pipeline.{AlertEngine, WindowMetrics}
import com.ankit.pipeline.domain.LogLevel

class AlertEngineSpec extends CatsEffectSuite:

  test("ERROR count > 2 pe alert trigger hona chahiye"):
    val metrics = WindowMetrics("payment-service", LogLevel.ERROR, 3, "30s")
    assert(AlertEngine.shouldAlert(metrics))

  test("ERROR count <= 2 pe alert nahi hona chahiye"):
    val metrics = WindowMetrics("payment-service", LogLevel.ERROR, 2, "30s")
    assert(!AlertEngine.shouldAlert(metrics))

  test("WARN count > 5 pe alert trigger hona chahiye"):
    val metrics = WindowMetrics("auth-service", LogLevel.WARN, 6, "30s")
    assert(AlertEngine.shouldAlert(metrics))

  test("WARN count <= 5 pe alert nahi hona chahiye"):
    val metrics = WindowMetrics("auth-service", LogLevel.WARN, 5, "30s")
    assert(!AlertEngine.shouldAlert(metrics))

  test("INFO pe kabhi alert nahi hona chahiye"):
    val metrics = WindowMetrics("any-service", LogLevel.INFO, 100, "30s")
    assert(!AlertEngine.shouldAlert(metrics))
