package com.ankit.pipeline.pipeline

import cats.effect.*
import fs2.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.client.Client
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import _root_.io.circe.syntax.*
import _root_.io.circe.generic.semiauto.*
import _root_.io.circe.*
import com.ankit.pipeline.domain.LogLevel

case class WebhookPayload(
  service: String,
  level: String,
  count: Long,
  message: String
)

object WebhookPayload:
  given Encoder[WebhookPayload] = deriveEncoder

object AlertEngine:

  val ERROR_THRESHOLD = 2L
  val WARN_THRESHOLD  = 5L

  def shouldAlert(metrics: WindowMetrics): Boolean =
    metrics.level match
      case LogLevel.ERROR => metrics.count > ERROR_THRESHOLD
      case LogLevel.WARN  => metrics.count > WARN_THRESHOLD
      case _              => false

  def alertPipe(client: Client[IO]): Pipe[IO, WindowMetrics, Unit] =
    _.filter(shouldAlert)
      .evalMap { metrics =>
        val payload = WebhookPayload(
          service = metrics.service,
          level   = metrics.level.toString,
          count   = metrics.count,
          message = s"ALERT: ${metrics.service} had ${metrics.count} ${metrics.level} logs!"
        )
        IO.println(s"🚨 ALERT TRIGGERED! ${payload.message}") *>
        sendWebhook(client, payload).handleErrorWith { err =>
          IO.println(s"⚠️ Webhook failed: ${err.getMessage}")
        }
      }

  private def sendWebhook(client: Client[IO], payload: WebhookPayload): IO[Unit] =
    val request = Request[IO](
      method = Method.POST,
      uri    = uri"https://webhook.site/your-webhook-id"
    ).withEntity(payload.asJson.toString)
      .withHeaders(
        headers.`Content-Type`(MediaType.application.json)
      )
    client.expect[String](request).void
