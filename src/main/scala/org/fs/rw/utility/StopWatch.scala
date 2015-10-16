package org.fs.rw.utility

/**
 * @author FS
 */
class StopWatch {
  var timestamp = System.currentTimeMillis

  def peek: Long =
    System.currentTimeMillis - timestamp

  def set(): Long = {
    val now = System.currentTimeMillis
    val res = now - timestamp
    timestamp = now
    res
  }
}

/**
 * @author FS
 */
object StopWatch {
  /** Execute code block and return block result along with time taken */
  def measure[R](block: => R): (R, Long) = {
    val sw = new StopWatch
    val res = block
    (res, sw.peek)
  }
}
