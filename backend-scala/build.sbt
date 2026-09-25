ThisBuild / scalaVersion := "3.5.2"
ThisBuild / version := "0.1.0"

import sbtassembly.MergeStrategy

lazy val root = (project in file("."))
  .settings(
    name := "backend-scala",
    Compile / PB.protoSources := Seq(file("../proto")),
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies ++= Seq(
      // Apache Pekko, not Akka: Akka relicensed to BUSL starting with 2.7+
      // and Lightbend stopped reliably publishing those versions to public
      // Maven Central (they require Lightbend's own commercial repository
      // and, per Akka's own community forum, even specific versions like
      // 2.9.3 are reported "not found" there too). Pekko is the Apache
      // Software Foundation's fork from before that relicensing — same
      // API (org.apache.pekko.* instead of akka.*), Apache 2.0, published
      // to plain Maven Central, and built with native Scala 3 support, so
      // no CrossVersion shim is needed either (unlike the Akka artifacts
      // this replaces).
      "org.apache.pekko" %% "pekko-http"        % "1.1.0",
      "org.apache.pekko" %% "pekko-stream"      % "1.1.3",
      "org.apache.pekko" %% "pekko-actor-typed" % "1.1.3",
      "io.spray"          %% "spray-json"        % "1.3.6"  cross CrossVersion.for3Use2_13,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
    ),
    // Fixed output name so the Dockerfile can COPY it by exact path — no
    // wildcard glob that could ambiguously match multiple jars (the same
    // class of bug that broke backend-kotlin's Docker image).
    assembly / assemblyJarName := "backend-scala.jar",
    assembly / mainClass := Some("com.example.webapi.Main"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) =>
        xs.map(_.toLowerCase) match {
          case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) => MergeStrategy.discard
          case ps if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") || ps.last.endsWith(".rsa") => MergeStrategy.discard
          case "services" :: _ => MergeStrategy.filterDistinctLines
          case _ => MergeStrategy.discard
        }
      case "reference.conf" => MergeStrategy.concat
      case "module-info.class" => MergeStrategy.discard
      case _ => MergeStrategy.first
    }
  )
