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
    private val PeriodMs = 2000

    private var historyStack: IndexedSeq[HistoryEntry] = IndexedSeq.empty
    private var currPartition: Option[NetworkPartition] = None

    require(NumEntriesToLook >= 3)

    override def run(): Unit = {
      while (!Thread.interrupted()) {
        val beforeTime = System.currentTimeMillis
        execNotFasterThan(PeriodMs) {
          try {
            val up = checker.isUp(PeriodMs - 150)
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
          val historyStackUp = historyStack.filter(_.up)
          val isUp = historyStackUp.size >= 3
          val isDown = historyStackUp.isEmpty
          currPartition match {
            case Some(partition) if isUp =>
              // Partition is over
              val restoreTimeMs = historyStackUp.last.timeMs
              val durationMs = restoreTimeMs - partition.startTime.getMillis
              log.debug(s"Network restored (${msToS(System.currentTimeMillis - restoreTimeMs)} seconds ago"
                + s", was broken for ${durationMs.hhMmSsString})")
              dao.updateNetworkPartition(partition.copy(
                endTimeOption  = Some(new DateTime(restoreTimeMs)),
                durationOption = Some(msToS(durationMs))
              ))
              currPartition = None
            case None if isDown =>
              // Partition!
              val startTimeMs = historyStack.last.timeMs
              log.debug(s"Network partition (${msToS(System.currentTimeMillis - startTimeMs)} seconds ago)!")
              currPartition = Some(dao.saveNetworkPartition(new DateTime(startTimeMs)))
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
