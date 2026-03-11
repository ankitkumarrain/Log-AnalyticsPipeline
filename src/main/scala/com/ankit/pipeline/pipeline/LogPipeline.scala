package com.ankit.pipeline.pipeline

import cats.effect.*
import fs2.*
import fs2.io.file.{Files, Path}
import com.ankit.pipeline.domain.*

object LogPipeline:

  // File se lines padhna
  def readLines(path: String): Stream[IO, String] =
    Files[IO]
      .readUtf8Lines(Path(path))
      .filter(_.nonEmpty)

  // Parse karna — valid aur invalid alag karna
  val parsePipe: Pipe[IO, String, Either[List[String], LogEntry]] =
    _.map { line =>
      Parser.parse(line) match
        case cats.data.Validated.Valid(entry)   => Right(entry)
        case cats.data.Validated.Invalid(errs)  => Left(errs.toList)
    }

  // Sirf valid entries
  val validOnly: Pipe[IO, Either[List[String], LogEntry], LogEntry] =
    _.collect { case Right(entry) => entry }

  // Sirf invalid entries — logging ke liye
  val invalidOnly: Pipe[IO, Either[List[String], LogEntry], List[String]] =
    _.collect { case Left(errs) => errs }

  // Complete pipeline
  def run(filePath: String): IO[Unit] =
    val parsed = readLines(filePath).through(parsePipe)

    // Valid entries print karo
    val validStream = parsed
      .through(validOnly)
      .evalMap(entry => IO.println(s"✅ [${entry.level}] ${entry.service} — ${entry.message}"))

    // Invalid entries print karo
    val invalidStream = parsed
      .through(invalidOnly)
      .evalMap(errs => IO.println(s"❌ Parse errors: ${errs.mkString(", ")}"))

    // Dono streams merge karke chalao
    validStream.merge(invalidStream).compile.drain
