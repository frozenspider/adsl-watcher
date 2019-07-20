package org.fs.rw

import java.net.SocketTimeoutException

import org.fs.rw.detection.Detector
import org.fs.rw.domain.DetectionError
import org.fs.rw.domain.Message
import org.slf4s.Logging
import org.apache.http.conn.util.InetAddressUtils

class DetectionExecutor(
  detectors: Seq[Detector]
) extends Logging {

  def detect(routerIp: String, username: String, password: String, interface: String): Message = {
    val routerIpErrorOption: Option[String] =
      if (!InetAddressUtils.isIPv4Address(routerIp)) {
        Some(s"$routerIp is not a valid IPv4 address")
      } else {
        None
      }
    routerIpErrorOption match {
      case None =>
        detectWithIp(routerIp)(username, password, interface)
      case Some(message) =>
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
