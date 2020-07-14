package org.fs.rw.database

import org.fs.rw.domain.Message
import org.fs.rw.domain.NetworkPartition
import org.joda.time.DateTime

object NoopDao extends Dao {
  override def setup(): Unit = {}

  override def saveMessage(message: Message): Unit = {}

  def saveNetworkPartition(startTime: DateTime): NetworkPartition = {
    NetworkPartition(startTime = DateTime.now())
  }

  def updateNetworkPartition(partition: NetworkPartition): Unit = {}

  override def tearDown(): Unit = {}
}
