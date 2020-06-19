package org.fs.rw.networkstate

import java.net.InetAddress

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Await
import scala.concurrent.duration._
import org.fs.rw.utility.Imports

class ReachableChecker extends NetworkStateChecker {
  private val threadName = "reachable-checker"
  private val host = "8.8.8.8"
  private val timeoutMs = 800
  private lazy val addr = InetAddress.getByName(host)

  private var thread: Thread = new Thread(threadName)

  override def isUp(): Boolean = {
    require(!thread.isAlive, "Thread is still alive!")
    val promise = Promise[Boolean]
    thread = new Thread(() => {
      try {
        promise.success(addr.isReachable(timeoutMs - 25))
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
        thread.interrupt()
        false
    }
  }
}

object ReachableChecker extends App with Imports {
  val checker = new ReachableChecker
  for (i <- 1 to 100) {
    execNotFasterThan(1000) {
      println(checker.isUp)
    }
  }
}
