import AssemblyKeys._

assemblySettings

jarName   in assembly := name.value + "-" + version.value + ".jar"

mainClass in assembly := Some("org.fs.rw.RouterWatcherMain")

outputPath in assembly <<= (jarName in assembly) map (jn => file(".") / jn)
