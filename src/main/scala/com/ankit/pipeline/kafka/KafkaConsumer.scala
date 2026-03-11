package com.ankit.pipeline.kafka

import cats.effect.*
import fs2.*
import fs2.kafka.*

object LogKafkaConsumer:

  def stream(bootstrapServers: String, topic: String): Stream[IO, String] =
    val settings = ConsumerSettings[IO, Option[String], Option[String]]
      .withBootstrapServers(bootstrapServers)
      .withGroupId("log-analytics-group-2")
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withEnableAutoCommit(true)

    KafkaConsumer
      .stream(settings)
      .subscribeTo(topic)
      .records
      .map(_.record.value)
      .collect { case Some(value) => value }
