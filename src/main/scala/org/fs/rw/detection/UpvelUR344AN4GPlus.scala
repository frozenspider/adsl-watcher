package org.fs.rw.detection

import org.fs.rw.domain.AnnexMode
import org.fs.rw.domain.DetectionError
import org.fs.rw.domain.Message
import org.fs.rw.domain.Modulation
import org.fs.rw.domain.RouterInfo
import org.fs.rw.utility.Imports._
import org.fs.rw.utility.StopWatch

import scalaj.http.Http

object UpvelUR344AN4GPlus extends Detector {

  val deviceInfoUri = "cgi-bin/status_deviceinfo.asp"

  override def getContent(routerIp: String,
                          username: String,
                          password: String): GetContentType = {
    val rootResponse = Http(s"http://$routerIp").asString
    if (rootResponse.code != 401 || !rootResponse.headers.contains("Set-Cookie")) {
      None
    } else {
      val cookie = rootResponse.headers("Set-Cookie")
      def AuthHttp(url: String) =
        Http(url).auth(username, password).header("Cookie", cookie)
      val authResponse = AuthHttp(s"http://$routerIp").asString
      if (authResponse.code != 200) {
        Some(Left(DetectionError.of("Invalid username or password")))
      } else if (!authResponse.body.contains("<TITLE>UR-344AN4G+</TITLE>")) {
        None
      } else {
        Some(Right(AuthHttp(s"http://$routerIp/$deviceInfoUri").asString.body))
      }
    }
  }

  override def parseContent(content: String): Message = {
    val (res, time) = StopWatch.measure {
      val body = parseElement(content) \ "body"
      val tableStateCellsText = {
        val tables = (body \ "form" \ "table")(_.child.size > 0)
        tables(1) \ "tr" \ "td" filterByClass "tabdata" map (_.cleanText)
      }
      val serverIpOption = {
        val statusBlock = body \ "form" \ "tr" \ "td"
        val statusIdx = statusBlock indexWhere (_.text.contains("Status"))
        parseServerIpOption((statusBlock(statusIdx + 7) \ "table" \ "tr" \ "td")(0).cleanText)
      }
      RouterInfo(
        timestamp = DateTime.now,
        firmwareOption = Some(tableStateCellsText(2)),
        //
        // Connection state
        //
        lineUpOption = Some(tableStateCellsText(5) == "up"),
        serverIpOption = serverIpOption,
        modulationOption = parseModulationOption(tableStateCellsText(8)),
        annexModeOption = parseAnnexOption(tableStateCellsText(11)),
        //
        // Connection characteristics
        //
        snrMarginOption = parseDecibellsOption(tableStateCellsText(21)),
        lineAttenuationOption = parseDecibellsOption(tableStateCellsText(26)),
        lineRateOption = parseDataRateOption(tableStateCellsText(31)),
        //
        // Error counters
        //
        crcErrorsOption = None,
        erroredSecondsOption = parseSecondsOption(tableStateCellsText(36)),
        severelyErroredSecondsOption = parseSecondsOption(tableStateCellsText(41)),
        unavailableSecondsOption = parseSecondsOption(tableStateCellsText(46))
      )
    }
    log.debug(s"Parsed in $time ms")
    res
  }

  private def parseServerIpOption(s: String): Option[String] = {
    notAvailableOr(s, s)
  }

  private def parseModulationOption(s: String): Option[Modulation] = {
    notAvailableOr(s, {
      require(s.contains("(ADSL"))
      Modulation.valueOf(s replaceAll ("[^(]+\\(", "") replace (")", ""))
    })
  }

  private def parseAnnexOption(s: String): Option[AnnexMode] = {
    notAvailableOr(s, {
      require(s.startsWith("ANNEX_"))
      AnnexMode.valueOf(s.dropRight(6))
    })
  }

  private def parseDecibellsOption(s: String): Option[Double] = {
    notAvailableOr(s, {
      require(s.endsWith(" dB"))
      s.dropRight(3).toDouble
    })
  }

  private def parseDataRateOption(s: String): Option[Int] = {
    notAvailableOr(s, {
      require(s.endsWith(" kbps"))
      s.dropRight(5).toInt
    })
  }

  private def parseSecondsOption(s: String): Option[Int] = {
    notAvailableOr(s, {
      s.toInt
    })
  }

  private def notAvailableOr[A](s: String, alternative: => A): Option[A] = {
    if (s == "N/A")
      None
    else
      Some(alternative)
  }
}
