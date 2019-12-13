logLevel := Level.Warn

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.8.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.12")

addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.4.0")

// dependencyUpdates: show a list of project dependencies that can be updated
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.4")
