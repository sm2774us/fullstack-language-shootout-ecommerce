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
      "com.typesafe.akka" %% "akka-http"         % "10.6.3" cross CrossVersion.for3Use2_13,
      "com.typesafe.akka" %% "akka-stream"       % "2.9.3"  cross CrossVersion.for3Use2_13,
      "com.typesafe.akka" %% "akka-actor-typed"  % "2.9.3"  cross CrossVersion.for3Use2_13,
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
