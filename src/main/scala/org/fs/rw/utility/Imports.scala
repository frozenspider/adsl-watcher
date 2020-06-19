package org.fs.rw.utility

trait Imports
    extends com.github.nscala_time.time.Imports
    with org.fs.utility.Imports
    with org.fs.utility.web.Imports {
  def now = DateTime.now

  def execNotFasterThan(minTimeMs: Long)(action: => Unit): Unit = {
    val startTime = System.currentTimeMillis
    action
    val endTime = System.currentTimeMillis
    val nextTime = startTime + minTimeMs
    Thread.sleep((nextTime - endTime) max 0L)
  }
}

object Imports extends Imports
