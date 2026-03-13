package com.ankit.pipeline

import munit.CatsEffectSuite
import com.ankit.pipeline.pipeline.Parser
import com.ankit.pipeline.domain.*
import cats.data.Validated.*

class ParserSpec extends CatsEffectSuite:

  test("valid log entry parse hona chahiye"):
    val raw = "2024-01-15T10:30:00Z|ERROR|payment-service|Connection timeout|trace-001"
    val result = Parser.parse(raw)
    assert(result.isValid)
    result match
      case Valid(entry) =>
        assertEquals(entry.level, LogLevel.ERROR)
        assertEquals(entry.service, "payment-service")
        assertEquals(entry.message, "Connection timeout")
        assertEquals(entry.traceId, Some("trace-001"))
      case _ => fail("Expected Valid")

  test("invalid timestamp invalidate hona chahiye"):
    val raw = "bad-timestamp|ERROR|service|message|trace"
    val result = Parser.parse(raw)
    assert(result.isInvalid)

  test("invalid log level invalidate hona chahiye"):
    val raw = "2024-01-15T10:30:00Z|UNKNOWN|service|message|trace"
    val result = Parser.parse(raw)
    assert(result.isInvalid)

  test("empty service invalidate hona chahiye"):
    val raw = "2024-01-15T10:30:00Z|ERROR|  |message|trace"
    val result = Parser.parse(raw)
    assert(result.isInvalid)

  test("multiple errors ek saath collect hone chahiye"):
    val raw = "bad-time|UNKNOWN|  |  "
    val result = Parser.parse(raw)
    result match
      case Invalid(errs) =>
        assert(errs.length > 1, "Multiple errors expected")
      case _ => fail("Expected Invalid")

  test("trace id optional hona chahiye"):
    val raw = "2024-01-15T10:30:00Z|INFO|auth-service|User logged in"
    val result = Parser.parse(raw)
    assert(result.isValid)
    result match
      case Valid(entry) =>
        assertEquals(entry.traceId, None)
      case _ => fail("Expected Valid")
