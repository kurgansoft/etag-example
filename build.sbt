ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.16"

val zioVersion = "2.1.19"

lazy val root = (project in file("."))
  .settings(
    name := "ETag Example",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-http" % "3.0.1",
      "dev.zio" %% "zio-test" % zioVersion % Test,
    )
  )