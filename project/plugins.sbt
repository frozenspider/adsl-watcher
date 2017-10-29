addSbtPlugin("com.eed3si9n" % "sbt-assembly"  % "0.14.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("org.flywaydb" % "flyway-sbt"    % "4.2.0")

// Since "https://flywaydb.org/repo" does not support SBT 1.0 yet
resolvers += "Flyway-alt" at "https://davidmweber.github.io/flyway-sbt.repo"
