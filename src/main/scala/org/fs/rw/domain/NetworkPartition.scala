package org.fs.rw.domain

import org.joda.time.DateTime

case class NetworkPartition(
  id:               Option[Int]      = None,
  startTime:        DateTime,
  endTimeOption:    Option[DateTime] = None,
  durationOption:   Option[Int]      = None, // In seconds
  logMessageOption: Option[String]   = None
) {
  def hasEnded: Boolean = {
    endTimeOption.nonEmpty
  }
}
