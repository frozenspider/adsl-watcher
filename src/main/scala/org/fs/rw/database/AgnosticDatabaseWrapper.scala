package org.fs.rw.database

import com.typesafe.config.Config

import com.typesafe.config.ConfigFactory

import slick.driver.H2Driver
import slick.driver.JdbcDriver
import slick.driver.MySQLDriver

class AgnosticDatabaseWrapper(dbKey: String, config: Config) {
  lazy val driverName = config.getString(s"$dbKey.driver")

  lazy val profile: JdbcDriver = {
    driverName match {
      case "org.h2.Driver"         => H2Driver
      case "com.mysql.jdbc.Driver" => MySQLDriver
    }
  }

  lazy val api = profile.api

  lazy val database = api.Database.forConfig(dbKey, config)
}
