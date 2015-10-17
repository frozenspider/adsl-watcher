package org.fs.rw

import java.io.File

import org.fs.rw.database.SlickDao
import org.fs.rw.detection._
import org.slf4s.Logging

import com.typesafe.config.ConfigFactory

object RouterWatcherMain extends App with Logging {

  val config = ConfigFactory.parseFileAnySyntax(new File("application.conf"))
  if (!config.hasPath("router")) {
    log.error("Config file not found or is invalid")
    System.exit(1)
  }

  val detectors = Seq(
    UpvelUR344AN4GPlus
  )
  val routerDiscoverer = new RouterDiscoverer()
  val executor = new DetectionExecutor(routerDiscoverer = routerDiscoverer, detectors = detectors)
  val dao = new SlickDao(config = config)
  scala.sys.addShutdownHook {
    dao.tearDown()
    log.error("Shutdown complete")
  }

  val iterator = new DetectionIterator(
    routerDiscoverer = routerDiscoverer,
    executor = executor,
    dao = dao,
    config = config
  )

  {
    import BuildInfo._
    log.info(s"$name v$version started, awaiting 10 seconds")
  }
  Thread.sleep(10 * 1000)
  iterator.start()
}
