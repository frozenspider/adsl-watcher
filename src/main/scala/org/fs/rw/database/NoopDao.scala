package org.fs.rw.database

import org.fs.rw.domain.Message
import org.fs.rw.domain.NetworkPartition
import org.joda.time.DateTime

object NoopDao extends Dao {
  private var partitionStack = List.empty[NetworkPartition]
  
  override def setup(): Unit = {}

  override def saveMessage(message: Message): Unit = {}

  def loadLatestPartition(): Option[NetworkPartition] = {
    partitionStack.headOption
  }

  def saveNetworkPartition(startTime: DateTime): NetworkPartition = {
    val np = NetworkPartition(startTime = DateTime.now())
    partitionStack = np +: partitionStack
    np
  }

  def finishNetworkPartition(id: Int, endTime: DateTime, duration: Int): Unit = {}

  override def tearDown(): Unit = {}
}
