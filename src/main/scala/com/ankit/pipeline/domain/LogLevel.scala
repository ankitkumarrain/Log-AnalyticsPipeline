package com.ankit.pipeline.domain

import io.circe.*

enum LogLevel:
  case DEBUG, INFO, WARN, ERROR, FATAL

object LogLevel:
  given Encoder[LogLevel] = Encoder[String].contramap(_.toString)
  given Decoder[LogLevel] = Decoder[String].emap { s =>
    LogLevel.values.find(_.toString == s.toUpperCase)
      .toRight(s"Invalid log level: $s")
  }
