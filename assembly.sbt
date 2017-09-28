mainClass          in assembly := Some("org.fs.rw.RouterWatcherMain")
assemblyJarName    in assembly := name.value + "-" + version.value + "b" + buildInfoBuildNumber.value + ".jar"
assemblyOutputPath in assembly := file("./_build") / (assemblyJarName in assembly).value
