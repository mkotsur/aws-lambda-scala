name := "aws-lambda-scala"
organization := "io.github.mkotsur"
// version := @see version.sbt

releasePublishArtifactsAction := PgpKeys.publishSigned.value

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

scalaVersion := "2.12.4"

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

fork in Test := true

javaOptions in Test ++= Seq("-Dfile.encoding=UTF-8")

val circeVersion = "0.9.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"

libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.0"

libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "2.0.2"

// Test dependencies

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

libraryDependencies += "org.mockito" % "mockito-core" % "2.15.0" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % "test"
