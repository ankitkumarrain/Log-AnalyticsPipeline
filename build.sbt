val scala3Version = "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "log-analytics-pipeline",
    version := "0.1.0",
    scalaVersion := scala3Version,
    Compile / run / fork := true,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core"   % "2.12.0",
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "co.fs2" %% "fs2-core" % "3.10.2",
      "co.fs2" %% "fs2-io"   % "3.10.2",
      "com.github.fd4s" %% "fs2-kafka" % "3.5.1",
      "org.http4s" %% "http4s-ember-server" % "0.23.27",
      "org.http4s" %% "http4s-ember-client" % "0.23.27",
      "org.http4s" %% "http4s-circe"        % "0.23.27",
      "org.http4s" %% "http4s-dsl"          % "0.23.27",
      "io.circe" %% "circe-core"    % "0.14.9",
      "io.circe" %% "circe-generic" % "0.14.9",
      "io.circe" %% "circe-parser"  % "0.14.9",
      "org.tpolecat" %% "doobie-core"     % "1.0.0-RC5",
      "org.tpolecat" %% "doobie-hikari"   % "1.0.0-RC5",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC5",
      "com.github.pureconfig" %% "pureconfig-core"        % "0.17.7",
      "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.17.7",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test
    )

  )