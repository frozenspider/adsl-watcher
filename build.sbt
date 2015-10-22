name         := "RouterWatcher"

version      := "1.2"

scalaVersion := "2.11.7"


EclipseKeys.withSource := true

EclipseKeys.createSrc  := EclipseCreateSrc.Default + EclipseCreateSrc.Resource + EclipseCreateSrc.Managed


libraryDependencies ++= Seq(
  // Logging
  "org.slf4s"              %% "slf4s-api"            % "1.7.12",
  "ch.qos.logback"         %  "logback-classic"      % "1.1.2",
  // Web
  "org.scalaj"             %% "scalaj-http"          % "1.1.5",
  "org.scala-lang.modules" %% "scala-xml"            % "1.0.5",
  "org.ccil.cowan.tagsoup" %  "tagsoup"              % "1.2.1",
  // Database
  "com.typesafe.slick"     %% "slick"                % "3.1.0",
  "mysql"                  %  "mysql-connector-java" % "5.1.36",
  // Other
  "org.apache.commons"     %  "commons-lang3"        % "3.4",
  "com.github.nscala-time" %  "nscala-time_2.11"     % "2.2.0",
  "com.typesafe"           %  "config"               % "1.3.0",
  // Test
  "junit"                  %  "junit"                % "4.11"  % "test",
  "org.scalatest"          %% "scalatest"            % "2.2.4" % "test"
)


buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

buildInfoPackage := "org.fs.rw"
