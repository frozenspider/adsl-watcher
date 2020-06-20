name         := "adsl-watcher"
version      := "1.7"
scalaVersion := "2.12.3"

sourceManaged            := baseDirectory.value / "src_managed"
sourceManaged in Compile := baseDirectory.value / "src_managed" / "main" / "scala"
sourceManaged in Test    := baseDirectory.value / "src_managed" / "test" / "scala"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, buildInfoBuildNumber),
    buildInfoPackage := "org.fs.rw"
  )

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
  // Logging
  "org.slf4s"               %% "slf4s-api"            % "1.7.25",
  "ch.qos.logback"          %  "logback-classic"      % "1.1.2",
  "org.slf4j"               %  "jcl-over-slf4j"       % "1.7.30",
  // Database
  "com.typesafe.slick"      %% "slick"                % "3.2.1",
  "mysql"                   %  "mysql-connector-java" % "5.1.36",
  "org.flywaydb"            %  "flyway-core"          % "4.2.0",
  // Other
  "com.github.frozenspider" %% "fs-common-utils"      % "0.1.3",
  "com.github.frozenspider" %% "fs-web-utils"         % "0.5.4.1",
  "org.apache.commons"      %  "commons-lang3"        % "3.4",
  "com.github.nscala-time"  %% "nscala-time"          % "2.16.0",
  "com.typesafe"            %  "config"               % "1.3.0",
  // Test
  "junit"                   %  "junit"                % "4.12"  % "test",
  "org.scalactic"           %% "scalactic"            % "3.0.4" % "test",
  "org.scalatest"           %% "scalatest"            % "3.0.4" % "test"
)
