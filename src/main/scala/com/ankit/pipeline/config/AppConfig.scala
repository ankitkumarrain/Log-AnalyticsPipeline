package com.ankit.pipeline.config

import pureconfig.*
import pureconfig.generic.derivation.default.*

case class KafkaConfig(
  bootstrapServers: String,
  topic: String,
  groupId: String
) derives ConfigReader

case class DatabaseConfig(
  driver: String,
  url: String,
  user: String,
  password: String
) derives ConfigReader

case class ServerConfig(
  host: String,
  port: Int
) derives ConfigReader

case class PipelineConfig(
  windowSeconds: Int,
  errorThreshold: Long,
  warnThreshold: Long
) derives ConfigReader

case class AppConfig(
  kafka: KafkaConfig,
  database: DatabaseConfig,
  server: ServerConfig,
  pipeline: PipelineConfig
) derives ConfigReader

object AppConfig:
  def load: Either[pureconfig.error.ConfigReaderFailures, AppConfig] =
    ConfigSource.default.at("app").load[AppConfig]
