addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.0")

// Provides the `scalapb` object (scalapb.gen(), scalapb.compiler.Version)
// referenced in build.sbt. sbt-protoc alone does not bring this in — it
// must be declared explicitly here so it's available on the sbt
// meta-build classpath. Confirmed via ScalaPB's own SBT Settings docs
// (scalapb.github.io/docs/sbt-settings) and sbt-protoc's README, both of
// which document this exact companion dependency for this exact usage.
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.11"
