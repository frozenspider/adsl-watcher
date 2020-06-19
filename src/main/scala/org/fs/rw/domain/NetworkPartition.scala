package org.fs.rw.domain

import org.joda.time.DateTime

case class NetworkPartition(
  id:               Option[Int]    = None,
  startTime:        DateTime,
  endTime:          DateTime,
  duration:         Int, // In seconds
  logMessageOption: Option[String]
)
