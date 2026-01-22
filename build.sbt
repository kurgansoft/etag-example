ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.18"

val zioVersion = "2.1.24"

lazy val root = (project in file("."))
  .settings(
    name := "ETag Example",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-http" % "3.8.0",
      "dev.zio" %% "zio-test" % zioVersion % Test
    ),
  )