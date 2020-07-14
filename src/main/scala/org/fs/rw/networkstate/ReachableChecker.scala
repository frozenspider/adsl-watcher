package org.fs.rw.networkstate

import java.net.InetAddress
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeoutException

import scala.concurrent.Await
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.Failure

import org.fs.rw.utility.Imports
import org.mozilla.universalchardet.UniversalDetector
import org.slf4s.Logging

class ReachableChecker extends NetworkStateChecker with Logging {
  private val threadName = "reachable-checker"
  private val host = "8.8.8.8"
  private lazy val addr = InetAddress.getByName(host)

  private val winsockRegex = "Unrecognized Windows Sockets error: (.+)".r

  private var thread: Thread = new Thread(threadName)

  override def isUp(timeoutMs: Int): Boolean = {
    require(!thread.isAlive, "Thread is still alive!")
    val promise = Promise[Boolean]
    thread = new Thread(() => {
      try {
        promise.success(addr.isReachable(timeoutMs - 50))
      } catch {
        case th: Throwable => promise.failure(th)
      }
    }, threadName)
    thread.start()
    val future = promise.future
    try {
      Await.result(future, timeoutMs.millis)
    } catch {
      case th: Throwable =>
        (future.value match {
          case Some(Failure(th2)) => th2
          case _                  => th
        }) match {
          case _: TimeoutException =>
            () // Logging a timeout is not too exciting
          case se: SocketException if se.getMessage.contains("Unrecognized Windows Sockets error") =>
            val winsockRegex(msg) = winsockRegex.findFirstIn(se.getMessage).get
            log.warn("Failed to check network status, winsock error: " + decodeWinsockMsg(msg))
          case th =>
            log.warn("Failed to check network status:", th)
        }
        thread.interrupt()
        false
    }
  }

  /** Windows Sockets error is erroneously interpreted as ISO-8859-1 */
  private def decodeWinsockMsg(msg: String): String = {
    val bytes = msg.getBytes(StandardCharsets.ISO_8859_1)
    val det = new UniversalDetector
    det.handleData(bytes)
    det.dataEnd()
    det.getDetectedCharset match {
      case null    => msg
      case charset => new String(bytes, charset)
    }
  }
}

object ReachableChecker extends App with Imports {
  val checker = new ReachableChecker
  for (i <- 1 to 100) {
    execNotFasterThan(1000) {
      println(checker.isUp(850))
    }
  }
}
