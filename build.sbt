import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging

scalacOptions ++= Seq("-feature", "-deprecation")
//javaOptions  ++= Seq("-Dconfig.trace=loads")

lazy val root = (project in file(".")).settings(
  name          := """sisdn""",
  version       := "1.0-Alpha",
  scalaVersion  := "2.11.7"
)

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

val akkaV       = "2.4.1"
val akkaStreamV = "2.0.1"
val scalaTestV  = "2.2.5"

libraryDependencies ++= Seq(
  "org.scala-lang.modules"          %% "scala-async"                          % "0.9.6-RC2",
  "com.typesafe.akka"               %% "akka-actor"                           % akkaV,
  "com.typesafe.akka"               %% "akka-testkit"                         % akkaV,
  "com.typesafe.akka"               %% "akka-persistence"                     % akkaV,
  "com.typesafe.akka"               %% "akka-persistence-query-experimental"  % akkaV,
  "com.typesafe.akka"               %% "akka-remote"                          % akkaV,
  "com.typesafe.akka"               %% "akka-stream-experimental"             % akkaStreamV,
  "com.typesafe.akka"               %% "akka-http-core-experimental"          % akkaStreamV,
  "com.typesafe.akka"               %% "akka-http-experimental"               % akkaStreamV,
  "com.typesafe.akka"               %% "akka-http-spray-json-experimental"    % akkaStreamV,
  "com.typesafe.akka"               %% "akka-http-testkit-experimental"       % akkaStreamV,
  //"com.typesafe.slick"              %% "slick"                                % "3.1.1",
  "com.typesafe"                    %  "config"                               % "1.3.0",
  "com.typesafe.slick"              %% "slick-hikaricp"                       % "3.1.1",
  //"org.slf4j"                       % "slf4j-nop"                             % "1.6.4",
  "org.scalatest"                   %% "scalatest"                            % scalaTestV % Test,
  "org.iq80.leveldb"                %  "leveldb"                              % "0.7",
  "org.fusesource.leveldbjni"       %  "leveldbjni-all"                       % "1.8",
  "com.jason-goodwin"               %% "authentikat-jwt"                      % "0.4.1",
  "com.github.dnvriend"             %% "akka-persistence-inmemory"            % "1.1.5" % Test,
  "com.google.protobuf"             %  "protobuf-java"                        % "2.5.0",
  "ch.qos.logback"                  %  "logback-classic"                      % "1.1.3",
  "com.nimbusds"                    %  "nimbus-jose-jwt"                      % "4.0.1",
  "com.h2database"                  %  "h2"                                   % "1.4.190",
  "mysql"                           % "mysql-connector-java"                  % "5.1.38"
  //"org.postgresql"                  %  "postgresql"                           % "9.4.1207"
  //"com.zaxxer"                      %  "HikariCP"                             % "2.4.1"
)

fork := true

/* for adding additional configs
fork in Test := true
javaOptions in Test += "-Dconfig.resource=test.conf"
*/

//mainClass in (Compile, run) := Some("sisdn.service.ServiceRoute")

//mainClass in (Compile, packageBin) := Some("sisdn.service.ServiceRoute")


enablePlugins(JavaServerAppPackaging)