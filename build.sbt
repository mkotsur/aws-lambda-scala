name := "aws-lambda-scala"
organization := "io.github.mkotsur"
// version := @see version.sbt

releasePublishArtifactsAction := PgpKeys.publishSigned.value

scalaVersion := "2.12.1"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

val circeVersion = "0.7.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.24"

libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.1.0"

libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "1.3.0"

// Test dependencies

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "org.mockito" % "mockito-core" % "2.2.22" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.11" % "test"
