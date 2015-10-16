package org.fs.rw

import org.fs.rw.domain.DetectionError
import org.fs.rw.domain.Message
import org.fs.rw.utility.Imports._
import org.fs.rw.detection.Detector

class DetectionExecutor(
    routerDiscoverer: RouterDiscoverer,
    detectors: Seq[Detector]) {

  def detect(username: String, password: String): Message = {
    val routerIpFork = routerDiscoverer.discoverIp()
    routerIpFork match {
      case Right(routerIp) =>
        detectors.toStream.flatMap(d =>
          d.detect(routerIp, username, password)
        ).headOption getOrElse {
          DetectionError(
            timestamp = now,
            message = "Unknown router type"
          )
        }
      case Left(message) =>
        DetectionError(
          timestamp = now,
          message = message
        )
    }
  }
}
