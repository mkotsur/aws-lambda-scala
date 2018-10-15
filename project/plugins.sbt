logLevel := Level.Warn

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.9")

addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.4.0")

// dependencyUpdates: show a list of project dependencies that can be updated
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.4")
