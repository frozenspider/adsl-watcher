package org.fs.rw.networkstate

import scala.io.StdIn

import org.fs.rw.database.Dao
import org.fs.rw.domain.NetworkPartition
import org.fs.rw.utility.Imports
import org.slf4s.Logging

class NetworkStateWatcher(
  checker: NetworkStateChecker,
  dao:     Dao
) extends Logging
  with Imports {

  def start(): Unit = {
    watcher.start()
  }

  private lazy val watcher = new Thread(new Runnable {

    /** We treat connection as fine if any of N last checks were successful */
    private val NumEntriesToLook = 5

    /** How often do we want to issue a network request? */
    private val PeriodMs = 1000

    private var historyStack: IndexedSeq[HistoryEntry] = IndexedSeq.empty
    private var partitionStart: Option[Long] = None

    require(NumEntriesToLook >= 2)

    override def run(): Unit = {
      while (!Thread.interrupted()) {
        val beforeTime = System.currentTimeMillis
        execNotFasterThan(PeriodMs) {
          try {
            val up = checker.isUp
            historyStack = (HistoryEntry(beforeTime, up) +: historyStack) take NumEntriesToLook
            historyUpdated()
          } catch {
            case th: Throwable => log.warn("Exception during iteration:", th)
          }
        }
      }
    }

    def historyUpdated() = {
      if (historyStack.length < NumEntriesToLook) {
        // Can't do anything yet
      } else {
        val maxDurationMs = historyStack.sliding(2).map(s => math.abs(s(0).timeMs - s(1).timeMs)).max
        if (maxDurationMs > (PeriodMs * 2)) {
          // We were e.g. hibernating or something, stack is invalid
          historyStack = IndexedSeq.empty
        } else {
          (partitionStart, historyStack.find(_.up)) match {
            case (Some(partitionStartTimeMs), Some(HistoryEntry(restoreTimeMs, _))) =>
              // Partition over, let's log it
              val durationMs = restoreTimeMs - partitionStartTimeMs
              log.debug(s"Network was broken for ${durationMs.hhMmSsString}!")
              val partition = NetworkPartition(
                startTime        = new DateTime(partitionStartTimeMs),
                endTime          = new DateTime(restoreTimeMs),
                duration         = msToS(durationMs),
                logMessageOption = None
              )
              dao.saveNetworkPartition(partition)
              partitionStart = None
            case (None, None) =>
              // Partition!
              val startTime = historyStack.last.timeMs
              log.debug(s"Network partition (${msToS(System.currentTimeMillis - startTime)} seconds ago)!")
              partitionStart = Some(startTime)
            case _ =>
              // State unchanged, NOOP
              ()
          }
        }
      }
    }

    private def msToS(ms: Long): Int = {
       math.round(ms / 1000.0d).toInt
    }

    private case class HistoryEntry(timeMs: Long, up: Boolean)
  }, "network-state-watcher")
}

object NetworkStateWatcher extends App {
  import org.fs.rw.database.NoopDao

  val checker = new ReachableChecker
  val watcher = new NetworkStateWatcher(checker, NoopDao)

  watcher.start()

  StdIn.readLine()
}
