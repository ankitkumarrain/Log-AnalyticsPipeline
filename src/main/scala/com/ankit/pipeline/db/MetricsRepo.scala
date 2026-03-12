package com.ankit.pipeline.db

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import com.ankit.pipeline.domain.*
import com.ankit.pipeline.pipeline.WindowMetrics
import java.time.Instant

object MetricsRepo:

  def insert(entry: LogEntry)(using xa: Transactor[IO]): IO[Unit] =
    sql"""
      INSERT INTO log_entries (timestamp, level, service, message, trace_id)
      VALUES (${entry.timestamp}, ${entry.level.toString}, ${entry.service}, ${entry.message}, ${entry.traceId})
    """.update.run.transact(xa).void

  def insertWindowMetrics(m: WindowMetrics)(using xa: Transactor[IO]): IO[Unit] =
    sql"""
      INSERT INTO alerts (service, level, count)
      VALUES (${m.service}, ${m.level.toString}, ${m.count})
    """.update.run.transact(xa).void

  def countByLevel(level: LogLevel)(using xa: Transactor[IO]): IO[Long] =
    sql"""
      SELECT COUNT(*) FROM log_entries WHERE level = ${level.toString}
    """.query[Long].unique.transact(xa)

  def recentErrors(limit: Int)(using xa: Transactor[IO]): IO[List[String]] =
    sql"""
      SELECT message FROM log_entries
      WHERE level = 'ERROR'
      ORDER BY timestamp DESC
      LIMIT $limit
    """.query[String].to[List].transact(xa)

  def getAlerts(using xa: Transactor[IO]): IO[List[(String, String, Long)]] =
    sql"""
      SELECT service, level, count FROM alerts
      ORDER BY created_at DESC
      LIMIT 20
    """.query[(String, String, Long)].to[List].transact(xa)
