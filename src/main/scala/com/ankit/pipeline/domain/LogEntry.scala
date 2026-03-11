package com.ankit.pipeline.domain

import io.circe.*
import io.circe.generic.semiauto.*
import java.time.Instant

case class LogEntry(
  timestamp: Instant,
  level: LogLevel,
  service: String,
  message: String,
  traceId: Option[String]
)

object LogEntry:
  given Encoder[Instant] = Encoder[String].contramap(_.toString)
  given Decoder[Instant] = Decoder[String].emap { s =>
    scala.util.Try(Instant.parse(s)).toEither.left.map(_.getMessage)
  }
  given Encoder[LogEntry] = deriveEncoder
  given Decoder[LogEntry] = deriveDecoder
