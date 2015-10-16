package org.fs.rw

import org.fs.rw.domain.DetectionError
import org.fs.rw.domain.Message
import org.fs.rw.utility.Imports._
import org.fs.rw.detection.Detector

class DetectionExecutor(
    routerDiscoverer: RouterDiscoverer,
    detectors: Seq[Detector]) {

  def detect(username: String, password: String): Message = {
    val routerIpOption = routerDiscoverer.discoverIp()
    routerIpOption map { routerIp =>
      detectors.toStream.flatMap(d =>
        d.detect(routerIp, username, password)
      ).headOption getOrElse {
        DetectionError(
          timestamp = now,
          message = "Unknown router type"
        )
      }
    } getOrElse {
      DetectionError(
        timestamp = now,
        message = "Can't determine router IP"
      )
    }
  }
}
