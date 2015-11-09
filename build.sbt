scalacOptions ++= Seq("-feature")

lazy val root = (project in file(".")).settings(
  name          := """sisdn""",
  version       := "1.0-Alpha",
  scalaVersion  := "2.11.7"
)

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

val akkaV       = "2.4.0"
val akkaStreamV = "1.0"
val scalaTestV  = "2.2.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka"               %% "akka-actor"                           % akkaV,
  "com.typesafe.akka"               %% "akka-testkit"                         % akkaV,
  "com.typesafe.akka"               %% "akka-persistence"                     % akkaV,
  "com.typesafe.akka"               %% "akka-remote"                          % akkaV,
  "com.typesafe.akka"               %% "akka-stream-experimental"             % akkaStreamV,
  "com.typesafe.akka"               %% "akka-http-core-experimental"          % akkaStreamV,
  "com.typesafe.akka"               %% "akka-http-experimental"               % akkaStreamV,
  "com.typesafe.akka"               %% "akka-http-spray-json-experimental"    % akkaStreamV,
  "com.typesafe.akka"               %% "akka-http-testkit-experimental"       % akkaStreamV,
  "org.scalatest"                   %% "scalatest"                            % scalaTestV % Test,
  "org.iq80.leveldb"                %  "leveldb"                              % "0.7",
  "org.fusesource.leveldbjni"       %  "leveldbjni-all"                       % "1.8",
  "com.jason-goodwin"               %% "authentikat-jwt"                      % "0.4.1",
  "com.github.dnvriend"             %% "akka-persistence-inmemory"            % "1.1.5" % Test,
  "com.google.protobuf"             % "protobuf-java"                         % "2.5.0"
)

fork := true