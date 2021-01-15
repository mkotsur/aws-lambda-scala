name := "aws-lambda-scala"
organization := "io.github.mkotsur"
// version := @see version.sbt

releasePublishArtifactsAction := PgpKeys.publishSigned.value

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.mavenLocalFile
  else
    Opts.resolver.sonatypeStaging
)

val scalaV211 = "2.11.12"
val scalaV212 = "2.12.13"
val scalaV213 = "2.13.4"
scalaVersion := scalaV213
crossScalaVersions := Seq(scalaV211, scalaV212, scalaV213)

import ReleaseTransformations._
releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

fork in Test := true

javaOptions in Test ++= Seq("-Dfile.encoding=UTF-8")

val circeVersion       = "0.12.3"
val circeVersionCompat = "0.11.2"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % (scalaVersion.value match {
  case `scalaV211` => circeVersionCompat
  case _           => circeVersion
}))

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"

libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.0"

libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "2.2.2"

// Test dependencies

libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0" % "test"

libraryDependencies += "org.mockito" %% "mockito-scala" % "1.10.0" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % "test"

libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.9.1" % "test"
