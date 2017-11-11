package org.fs.rw

import java.net.SocketTimeoutException

import org.fs.rw.detection.Detector
import org.fs.rw.domain.DetectionError
import org.fs.rw.domain.Message
import org.slf4s.Logging

class DetectionExecutor(
  routerDiscoverer: RouterDiscoverer,
  detectors:        Seq[Detector]
) extends Logging {

  def detect(username: String, password: String, interface: String): Message = {
    val routerIpFork = routerDiscoverer.discoverIp()
    routerIpFork match {
      case Right(routerIp) =>
        detectWithIp(routerIp)(username, password, interface)
      case Left(message) =>
        log.warn(message)
        DetectionError.of(message)
    }
  }

  private def detectWithIp(routerIp: String)(username: String, password: String, interface: String): Message = {
    try {
      detectors.toStream.flatMap(d =>
        d.detect(routerIp, username, password, interface)).headOption getOrElse {
        DetectionError.of("Unknown router type")
      }
    } catch {
      case ex: SocketTimeoutException =>
        DetectionError.of("Router query timeout")
    }
  }
}
