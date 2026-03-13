package com.ankit.pipeline.api

import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.circe.*
import org.http4s.headers.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.*
import doobie.*
import com.ankit.pipeline.db.MetricsRepo
import com.ankit.pipeline.domain.LogLevel
import scala.io.Source

case class MetricsResponse(errorCount: Long, warnCount: Long, infoCount: Long, debugCount: Long)
object MetricsResponse:
  given Encoder[MetricsResponse] = deriveEncoder

case class ErrorsResponse(recentErrors: List[String])
object ErrorsResponse:
  given Encoder[ErrorsResponse] = deriveEncoder

case class AlertEntry(service: String, level: String, count: Long)
object AlertEntry:
  given Encoder[AlertEntry] = deriveEncoder

case class AlertsResponse(alerts: List[AlertEntry])
object AlertsResponse:
  given Encoder[AlertsResponse] = deriveEncoder

object HttpApi:

  def routes(using xa: Transactor[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {

      case GET -> Root =>
        val html = Source.fromResource("dashboard.html").mkString
        Ok(html).map(_.withContentType(`Content-Type`(MediaType.text.html)))

      case GET -> Root / "health" =>
        Ok("Pipeline is running!")

      case GET -> Root / "metrics" =>
        for
          errors <- MetricsRepo.countByLevel(LogLevel.ERROR)
          warns  <- MetricsRepo.countByLevel(LogLevel.WARN)
          infos  <- MetricsRepo.countByLevel(LogLevel.INFO)
          debugs <- MetricsRepo.countByLevel(LogLevel.DEBUG)
          resp   <- Ok(MetricsResponse(errors, warns, infos, debugs).asJson)
        yield resp

      case GET -> Root / "errors" =>
        for
          errs <- MetricsRepo.recentErrors(10)
          resp <- Ok(ErrorsResponse(errs).asJson)
        yield resp

      case GET -> Root / "alerts" =>
        for
          rawAlerts <- MetricsRepo.getAlerts
          alerts = rawAlerts.map { case (svc, lvl, cnt) => AlertEntry(svc, lvl, cnt) }
          resp <- Ok(AlertsResponse(alerts).asJson)
        yield resp
    }
