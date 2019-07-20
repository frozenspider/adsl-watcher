package org.fs.rw

import java.net.InetAddress
import java.net.SocketTimeoutException

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.fs.rw.detection.Detector
import org.fs.rw.domain.DetectionError
import org.fs.rw.domain.Message
import org.slf4s.Logging

class DetectionExecutor(
  detectors: Seq[Detector]
) extends Logging {

  def detect(host: String, username: String, password: String, interface: String): Message = {
    Try(InetAddress.getByName(host)) match {
      case Success(addr) =>
        detectWithIp(addr.getHostAddress)(username, password, interface)
      case Failure(ex) =>
        val message = ex.getMessage
        log.warn(message)
        DetectionError.of(message)
    }
  }

  private def detectWithIp(deviceIp: String)(username: String, password: String, interface: String): Message = {
    try {
      detectors.toStream.flatMap(d =>
        d.detect(deviceIp, username, password, interface)).headOption getOrElse {
        DetectionError.of("Unknown device type")
      }
    } catch {
      case ex: SocketTimeoutException =>
        DetectionError.of("Device query timeout")
    }
  }
}
