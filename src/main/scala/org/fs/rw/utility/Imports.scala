package org.fs.rw.utility

import java.net.NetworkInterface

import scala.collection.JavaConverters._

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

  def networkInterfaces: Seq[NetworkInterface] = {
    NetworkInterface.getNetworkInterfaces.asScala.toSeq
  }
}

object Imports extends Imports
