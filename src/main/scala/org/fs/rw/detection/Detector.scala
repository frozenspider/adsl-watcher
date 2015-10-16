package org.fs.rw.detection

import org.fs.rw.domain.Message
import org.slf4s.Logging

import com.github.nscala_time.time.Imports._

trait Detector extends Logging {
  def detect(routerIp: String, username: String, password: String): Option[Message]
}
