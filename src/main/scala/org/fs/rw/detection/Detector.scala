package org.fs.rw.detection

import java.io.File
import java.io.IOException
import java.io.PrintWriter

import org.fs.rw.domain.DetectionError
import org.fs.rw.domain.Message
import org.fs.rw.utility.Imports._
import org.slf4s.Logging

trait Detector extends Logging {

  /**
   * Response from content fetching function.
   * Meaning is as follows:
   * <table>
   *  <tr>
   *   <td>{@code None}</td>
   *   <td>Router model does not belong to this detector</td>
   *  </tr>
   *  <tr>
   *   <td>{@code Some(Left(msg))}</td>
   *   <td>Content fetching fails, might mean configuration error or application bug</td>
   *  </tr>
   *  <tr>
   *    <td>{@code Some(Right(content))}</td>
   *    <td>Content fetched and returned</td>
   *  </tr>
   * </table>
   */
  type GetContentType = Option[Either[Message, String]]

  /** Detect a router connection values, returning None if router model belong to this detector */
  def detect(routerIp: String, username: String, password: String, interface: String): Option[Message] = {
    val content: Either[Throwable, GetContentType] =
      try {
        Right(getContent(routerIp, username, password, interface))
      } catch {
        case ex: Throwable => Left(ex)
      }
    content match {
      case Left(ex: IOException) =>
        Some(DetectionError.of("Failed to load content: " + ex))
      case Left(ex) =>
        throw ex
      case Right(None) =>
        None
      case Right(Some(Left(message))) =>
        Some(message)
      case Right(Some(Right(content))) =>
        try {
          Some(parseContent(content))
        } catch {
          case ex: Throwable =>
            dump(content)
            throw ex
        }
    }
  }

  /**
   * Get the router content
   *
   * @throws IOException indicates (somewhat expected) failure to load content
   * @throws Exception indicates (unexpected) internal error
   */
  def getContent(routerIp: String, username: String, password: String, interface: String): GetContentType

  def parseContent(content: String): Message

  private def dump(content: String): Unit = {
    val filename = s"${now.toString("yyyy-MM-dd_HH-mm")}_${getClass.getSimpleName}.txt"
    val file = new File(filename)
    val pw = new PrintWriter(file, "UTF-8")
    try {
      pw.write(content)
      pw.flush()
    } finally {
      pw.close()
    }
    log.info(s"Dumped router content to ${file.getAbsolutePath}")
  }
}
