package org.fs.rw

class RouterDiscoverer {
  def discoverIp(): Either[String, String] =
    System.getProperty("os.name") match {
      case v if v.toLowerCase startsWith "windows" =>
        discoverForWindows()
      case v =>
        Left(s"Unsupported OS: $v")
    }

  private def isNonSeparatorLine(s: String) = !s.startsWith("=====")

  private def executeNetstat(): String = {
    import scala.sys.process._
    "netstat -rn".!!
  }

  private def discoverForWindows(): Either[String, String] = {
    val netstatOutput = executeNetstat()
    val lines = netstatOutput.lines.toList
    if (lines.size < 0 || !lines.head.startsWith("=====")) {
      Left("Unexpected output format of 'netstat -rn'")
    } else {
      val linesParts = lines
        .drop(1).dropWhile(isNonSeparatorLine)
        .drop(4).takeWhile(isNonSeparatorLine)
        .map(_.trim.split("\\s+").toSeq)
      val linePartsOption = linesParts.find(s => s.contains("0.0.0.0") || s.contains("default"))
      linePartsOption.map { parts =>
        if (parts.size == 5) {
          Right(parts(2))
        } else {
          Left("Unexpected format of IPs for 'netstat -rn'")
        }
      } getOrElse {
        Left("Can't find the router address, possibly Windows network connection offline")
      }
    }
  }
}

object RouterDiscoverer extends App {
  val instance = new RouterDiscoverer
  println(instance.discoverIp())
}
