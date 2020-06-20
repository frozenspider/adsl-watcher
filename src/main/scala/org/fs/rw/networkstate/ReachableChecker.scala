package org.fs.rw.networkstate

import java.net.InetAddress
import java.util.concurrent.TimeoutException

import scala.concurrent.Await
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.Failure

import org.fs.rw.utility.Imports
import org.slf4s.Logging

class ReachableChecker extends NetworkStateChecker with Logging {
  private val threadName = "reachable-checker"
  private val host = "8.8.8.8"
  private val timeoutMs = 850
  private lazy val addr = InetAddress.getByName(host)

  private var thread: Thread = new Thread(threadName)

  override def isUp(): Boolean = {
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
          case _: TimeoutException => // Logging a timeout is not too exciting
          case th                  => log.warn("Failed to check network status:", th)
        }
        thread.interrupt()
        false
    }
  }
}

object ReachableChecker extends App with Imports {
  val nis = Imports.networkInterfaces
//  nis.sortBy(_.getName).foreach(println)
//  for (ni <- nis) {
//    val checker = new ReachableChecker(Some(ni))
//    if (checker.isUp) {
//      println(ni)
//    }
//  }
  val checker = new ReachableChecker
  for (i <- 1 to 100) {
    execNotFasterThan(1000) {
      println(checker.isUp)
    }
  }
}
