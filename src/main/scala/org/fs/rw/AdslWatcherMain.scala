package org.fs.rw

import java.io.File

import org.fs.rw.database.SlickDao
import org.fs.rw.detection._
import org.fs.rw.networkstate._
import org.slf4s.Logging

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object AdslWatcherMain extends App with Logging {

  val config: Config = ConfigFactory.parseFileAnySyntax(new File("application.conf"))
  if (!config.hasPath("db")) {
    log.error("Config file not found or is invalid")
    scala.sys.exit(1)
  }

  val dao: SlickDao = new SlickDao(config = config)
  scala.sys.addShutdownHook {
    dao.tearDown()
    log.info("Shutdown complete")
  }

  val iteratorOption: Option[DetectionIterator] =
    if (config.hasPath("device")) {
      val detectors: Seq[Detector] = Seq(
        UpvelUR344AN4GPlus, TendaD820B
      )
      val executor: org.fs.rw.DetectionExecutor = new DetectionExecutor(detectors = detectors)

      Some(new DetectionIterator(
        executor = executor,
        dao      = dao,
        config   = config
      ))
    } else {
      log.info("No device config, won't monitor ADSL state")
      None
    }

  val netWatcher = {
    val checker: NetworkStateChecker = new ReachableChecker
    new NetworkStateWatcher(
      checker = checker,
      dao     = dao
    )
  }

  {
    import BuildInfo._
    log.info(s"$name v$version b${buildInfoBuildNumber} started, awaiting 10 seconds")
  }
  Thread.sleep(10 * 1000)
  iteratorOption.map(_.start())
  netWatcher.start()
}
