package org.fs.rw.domain

import org.fs.rw.utility.Imports._

case class DetectionError(
  id: Option[Int] = None,
  timestamp: DateTime,
  message: String) extends Message
