package org.fs.rw.database

import org.fs.rw.domain.Message
import org.fs.rw.domain.NetworkPartition
import org.joda.time.DateTime

trait Dao {
  /**
   * Do the initial preparations, called once upon app startup.
   * Throwing exception from here will terminate the app.
   */
  def setup(): Unit

  /** Persist the given message */
  def saveMessage(message: Message): Unit

  def saveNetworkPartition(startTime: DateTime): NetworkPartition

  def updateNetworkPartition(partition: NetworkPartition): Unit

  /** Terminate all open connections and stuff, safe to call multiple times */
  def tearDown(): Unit
}
