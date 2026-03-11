package com.ankit.pipeline.domain

import io.circe.*
import io.circe.generic.semiauto.*
import java.time.Instant

case class Alert(
  service: String,
  level: LogLevel,
  count: Long,
  windowStart: Instant,
  windowEnd: Instant,
  message: String
)

object Alert:
  given Encoder[Instant] = Encoder[String].contramap(_.toString)
  given Encoder[Alert]   = deriveEncoder
  given Decoder[Alert]   = deriveDecoder
