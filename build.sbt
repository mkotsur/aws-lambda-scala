
name := "aws-lambda-scala"
organization := "io.github.mkotsur"
version := "0.0.2"

scalaVersion := "2.12.0"

val circeVersion = "0.6.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.1.0"

libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "1.1.0"

libraryDependencies += "com.amazonaws" % "aws-lambda-java-log4j" % "1.0.0"

libraryDependencies += "log4j" % "log4j" % "1.2.17"