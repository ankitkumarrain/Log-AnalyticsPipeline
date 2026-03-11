package com.ankit.pipeline.pipeline

import com.ankit.pipeline.domain.*
import cats.data.ValidatedNel
import cats.syntax.all.*
import java.time.Instant

object Parser:

  type ParseResult = ValidatedNel[String, LogEntry]

  def parse(raw: String): ParseResult =
    val parts = raw.trim.split("\\|", -1)
    if parts.length < 4 then
      s"Invalid format, expected 4+ fields, got: ${parts.length}".invalidNel
    else
      (
        parseTimestamp(parts(0)),
        parseLevel(parts(1)),
        parseService(parts(2)),
        parseMessage(parts(3)),
        parseTraceId(parts.lift(4))
      ).mapN(LogEntry.apply)

  private def parseTimestamp(s: String): ValidatedNel[String, Instant] =
    scala.util.Try(Instant.parse(s.trim))
      .toEither
      .leftMap(_ => s"Invalid timestamp: $s")
      .toValidatedNel

  private def parseLevel(s: String): ValidatedNel[String, LogLevel] =
    scala.util.Try(LogLevel.valueOf(s.trim.toUpperCase))
      .toEither
      .leftMap(_ => s"Invalid log level: $s")
      .toValidatedNel

  private def parseService(s: String): ValidatedNel[String, String] =
    val trimmed = s.trim
    if trimmed.isEmpty then "Service name cannot be empty".invalidNel
    else trimmed.validNel

  private def parseMessage(s: String): ValidatedNel[String, String] =
    val trimmed = s.trim
    if trimmed.isEmpty then "Message cannot be empty".invalidNel
    else trimmed.validNel

  private def parseTraceId(s: Option[String]): ValidatedNel[String, Option[String]] =
    s.map(_.trim).filter(_.nonEmpty).validNel
