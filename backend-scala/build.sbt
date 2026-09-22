ThisBuild / scalaVersion := "3.5.2"
ThisBuild / version := "0.1.0"

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
    )
  )
