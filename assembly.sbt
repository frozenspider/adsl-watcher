jarName    in assembly := name.value + "-" + version.value + "b" + buildInfoBuildNumber.value + ".jar"
mainClass  in assembly := Some("org.fs.rw.RouterWatcherMain")
outputPath in assembly <<= (assemblyJarName in assembly) map (jn => file("./_build") / jn)
