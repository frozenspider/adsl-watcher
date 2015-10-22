package org.fs.rw.detection

import java.io.File
import java.io.PrintWriter

import org.fs.rw.domain.Message
import org.fs.rw.utility.Imports._
import org.slf4s.Logging

trait Detector extends Logging {
  def detect(routerIp: String, username: String, password: String): Option[Message] = {
    val content = getContent(routerIp, username, password)
    content match {
      case None =>
        None
      case Some(Left(message)) =>
        Some(message)
      case Some(Right(content)) =>
        try {
          Some(parseContent(content))
        } catch {
          case ex: Throwable =>
            dump(content)
            throw ex
        }
    }
  }

  def getContent(routerIp: String, username: String, password: String): Option[Either[Message, String]]

  def parseContent(content: String): Message

  private def dump(content: String): Unit = {
    val filename = s"content-${getClass.getSimpleName}-${now.toString("yyyy-MM-dd_HH-mm")}.txt"
    val file = new File(filename)
    log.info(s"Dumped router content to ${file.getAbsolutePath}")
    val pw = new PrintWriter(file, "UTF-8")
    try {
      pw.write(content)
    } finally {
      pw.close()
    }
  }
}
