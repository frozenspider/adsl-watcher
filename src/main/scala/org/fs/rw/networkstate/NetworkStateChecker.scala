package org.fs.rw.networkstate

trait NetworkStateChecker {
  def isUp(timeoutMs: Int): Boolean
}
