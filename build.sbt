name := "aws-lambda-scala"
organization := "io.github.mkotsur"
// version := @see version.sbt

//TODO: check https://github.com/PlayQ/d4s/blob/master/build.sbt
// for some good ideas!

// Settings bundles.
// See: https://www.scala-sbt.org/1.x/docs/Multi-Project.html
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / resolvers += Resolver.bintrayRepo("sbt", "sbt-plugin-releases")

releasePublishArtifactsAction := PgpKeys.publishSigned.value

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.mavenLocalFile
  else
    Opts.resolver.sonatypeStaging
)

import ReleaseTransformations._
import sbt.Keys.libraryDependencies
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

// Test dependencies
lazy val `core` = project
  .in(file("core"))
  .settings(
    libraryDependencies ++= Seq(
      "org.slf4j"      % "slf4j-api"       % "1.7.25",
    ) ++ deps.circeAll ++ deps.testAll :+ deps.awsLambdaCore :+ deps.awsLambdaEvents
  )
  .settings(CompilerPlugins)

lazy val `eff-cats-effect` = project
  .in(file("effects/cats-effect"))
  .settings(
    libraryDependencies ++= Seq(
      deps.catsEffect % "provided"
    ) ++ deps.testAll
  )
  .dependsOn(`core`)

lazy val `example-cats-effect` = project
  .in(file("examples/cats-effect"))
  .settings(
    libraryDependencies ++= Seq(
      deps.catsEffect
    )
  )
  .settings(SimplePaths: _*)
  .dependsOn(`eff-cats-effect`)

lazy val deps = new {
  private lazy val V = new {
    val catsEffect      = "2.1.2"
    val pureconfig      = "0.12.2"
    val awsLambdaCore   = "1.2.0"
    val awsLambdaEvents = "2.2.2"
    val circe           = "0.12.3"
  }

  val catsEffect     = "org.typelevel"         %% "cats-effect"            % V.catsEffect
  val pureconfig     = "com.github.pureconfig" %% "pureconfig"             % V.pureconfig
  val pureconfigCats = "com.github.pureconfig" %% "pureconfig-cats-effect" % V.pureconfig

  val awsLambdaCore   = "com.amazonaws" % "aws-lambda-java-core"   % V.awsLambdaCore
  val awsLambdaEvents = "com.amazonaws" % "aws-lambda-java-events" % V.awsLambdaEvents

  val circeAll = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % V.circe)

  val testAll = Seq(
    "org.scalatest"  %% "scalatest"      % "3.1.0"  % "test",
    "org.mockito"    %% "mockito-scala"  % "1.10.0" % "test",
    "ch.qos.logback" % "logback-classic" % "1.2.3"  % "test"
  )
}

lazy val SimplePaths = Seq(
  Compile / scalaSource := baseDirectory.value / "src",
  Compile / resourceDirectory := baseDirectory.value / "res"
)

lazy val CompilerPlugins = Seq(
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
)
