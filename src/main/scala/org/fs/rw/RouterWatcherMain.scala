package org.fs.rw

import java.io.File

import org.fs.rw.database.SlickDao
import org.fs.rw.detection._
import org.slf4s.Logging

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object RouterWatcherMain extends App with Logging {

  val config: Config = ConfigFactory.parseFileAnySyntax(new File("application.conf"))
  if (!config.hasPath("router")) {
    log.error("Config file not found or is invalid")
    scala.sys.exit(1)
  }

  val detectors: Seq[Detector] = Seq(
    UpvelUR344AN4GPlus
  )
  val executor: org.fs.rw.DetectionExecutor = new DetectionExecutor(detectors = detectors)
  val dao: SlickDao = new SlickDao(config = config)
  scala.sys.addShutdownHook {
    dao.tearDown()
    log.info("Shutdown complete")
  }

  val iterator: DetectionIterator = new DetectionIterator(
    executor = executor,
    dao      = dao,
    config   = config
  )

  {
    import BuildInfo._
    log.info(s"$name v$version b${buildInfoBuildNumber} started, awaiting 10 seconds")
  }
  Thread.sleep(10 * 1000)
  iterator.start()
}
