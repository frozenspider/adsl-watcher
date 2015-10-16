package org.fs.rw.database

import org.fs.rw.domain.Message

trait Dao {
  def setup(): Unit

  def saveMessage(message: Message): Unit

  def tearDown(): Unit
}
