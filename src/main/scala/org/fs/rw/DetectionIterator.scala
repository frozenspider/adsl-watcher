package org.fs.rw

import java.lang.Thread.UncaughtExceptionHandler

import org.fs.rw.database.Dao
import org.fs.rw.utility.Imports._
import org.fs.rw.utility.StopWatch
import org.slf4s.Logging

import com.typesafe.config.Config

class DetectionIterator(
  routerDiscoverer: RouterDiscoverer,
  executor: DetectionExecutor,
  dao: Dao,
  config: Config)
    extends Logging {

  var threadOption: Option[Thread] = None

  def start(): Unit = {
    val periodMs = config.getInt("period")
    val username = config.getString("router.username")
    val password = config.getString("router.password")

    log.info("Started")
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        try {
          while (!Thread.interrupted) {
            val (_, passedMs) = StopWatch.measure {
              iteration()
            }
            val toWaitMs = periodMs - passedMs
            Thread.sleep(if (toWaitMs > 0) toWaitMs else 0)
          }
        } finally {
          dao.tearDown()
        }
      }

      def iteration(): Unit = {
        val message = executor.detect(username, password)
        dao.saveMessage(message)
      }
    })
    thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler {
      override def uncaughtException(thread: Thread, ex: Throwable): Unit =
        log.error("Exception in thread execution", ex)
    })
    dao.setup()
    thread.start()
    threadOption = Some(thread)
  }

  def stop(): Unit = {
    threadOption.map(_.interrupt())
    log.info("Stopped")
  }
}
