logLevel := Level.Warn

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.5")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.1.1")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")

addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.4.0")

// dependencyUpdates: show a list of project dependencies that can be updated
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.4")
