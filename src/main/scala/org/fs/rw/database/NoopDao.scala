package org.fs.rw.database

import org.fs.rw.domain.Message
import org.fs.rw.domain.NetworkPartition

object NoopDao extends Dao {
  override def setup(): Unit = {}

  override def saveMessage(message: Message): Unit = {}

  override def saveNetworkPartition(partition: NetworkPartition): Unit = {}

  override def tearDown(): Unit = {}
}
