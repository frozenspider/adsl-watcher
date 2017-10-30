package org.fs.rw

import java.lang.Thread.UncaughtExceptionHandler

import org.fs.rw.database.Dao
import org.fs.utility.StopWatch
import org.slf4s.Logging

import com.typesafe.config.Config

class DetectionIterator(
  routerDiscoverer: RouterDiscoverer,
  executor:         DetectionExecutor,
  dao:              Dao,
  config:           Config
)
    extends Logging {

  var threadOption: Option[Thread] = None

  def start(): Unit = {
    val detailedPeriodMs = config.getInt("period.detailed")
    val longtermPeriodMs = config.getInt("period.longterm")
    val storeDetailedForMs = config.getInt("period.storeDetailedFor")
    val username = config.getString("router.username")
    val password = config.getString("router.password")
    require(
      detailedPeriodMs < longtermPeriodMs / 2,
      "Detailed period should be more than two times smaller than long-term period"
    )

    log.info("Started")
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        try {
          while (!Thread.interrupted) {
            val (_, passedMs) = StopWatch.measure {
              iteration()
            }
            val waitTime = calculateWaitTime(passedMs)
            Thread.sleep(waitTime)
          }
        } finally {
          dao.tearDown()
        }
      }

      def calculateWaitTime(passedMs: Long): Long = {
        val toWaitMs = detailedPeriodMs - passedMs
        if (toWaitMs > 0) toWaitMs else 0
      }

      def iteration(): Unit = {
        val message = executor.detect(username, password)
        dao.saveMessage(message)
        dao.thinOut(storeDetailedForMs, longtermPeriodMs)
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
