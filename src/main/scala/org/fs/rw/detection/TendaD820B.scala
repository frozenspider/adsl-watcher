package org.fs.rw.detection

import scala.annotation.tailrec
import scala.xml.Node

import org.fs.rw.domain.AnnexMode
import org.fs.rw.domain.DetectionError
import org.fs.rw.domain.Message
import org.fs.rw.domain.Modulation
import org.fs.rw.domain.RouterInfo
import org.fs.rw.domain.RouterStream
import org.fs.rw.utility.Imports._
import org.fs.utility.StopWatch

object TendaD820B extends Detector {
  val timeoutMs = 60 * 1000

  val deviceInfoUri: String = "MainPage?id=10"

  val (client, cookieStore) = simpleClientWithStore()

  override def getContent(
    routerIp:  String,
    username:  String,
    password:  String,
    interface: String
  ): GetContentType = {
    val rootResponse = client.request(GET(s"http://$routerIp").addTimeout(timeoutMs))
    if (cookieStore.cookies.isEmpty) {
      None
    } else {
      if (rootResponse.code != 200) {
        // Make sure that auth is cached in cookies
        client.request(GET(s"http://$routerIp").addBasicAuth(username, password).addTimeout(timeoutMs))
      }
      // Funny thing is... router doesn't actually care about auth. Well fkin played, Upvel.
      val authRequest = POST(s"http://$routerIp/$deviceInfoUri").addParameter("DvInfo_PVC", interface).addTimeout(timeoutMs)
      val authResponse = client.request(authRequest)
      if (authResponse.code == 401) {
        Some(Left(DetectionError.of("Invalid username or password")))
      } else if (!authResponse.bodyString.contains("<TITLE>UR-344AN4G+</TITLE>")) {
        None
      } else {
        Some(Right(authResponse.bodyString))
      }
    }
  }

  override def parseContent(content: String): Message = {
    require(content.contains("TENDA, Inc. All rights reserved."), "Wrong router!")
    val (res, time) = StopWatch.measure {
      val body = (parseElement(content) \ "body").head
      val tables = body \ "form" \\ "table"
      val deviceInfoTable = tables(0)
      val dslInfoTable = tables(1)
      val dslStatusTable = tables(2)
      val countersTable = tables(3)
      val lineUp = (dslStatusTable \\ "td")(1).trimmedText startsWith "Showtime/Data"
      RouterInfo(
        timestamp      = DateTime.now,
        firmwareOption = Some((deviceInfoTable \\ "td")(1).trimmedText),
        //
        // Connection state
        //
        lineUpOption     = Some(lineUp),
        serverIpOption   = None,
        modulationOption = parseModulationOption((deviceInfoTable \\ "td")(11).trimmedText),
        annexModeOption  = None,
        //
        // Connection characteristics
        //
        downstream = RouterStream(
          snrMarginOption              = noneIfDownAndZero(lineUp, parseDoubleOption((dslStatusTable \\ "td")(18).trimmedText)),
          lineAttenuationOption        = noneIfDownAndZero(lineUp, parseDoubleOption((dslStatusTable \\ "td")(15).trimmedText)),
          dataRateOption               = noneIfDownAndZero(lineUp, parseIntOption((dslStatusTable \\ "td")(10).trimmedText)),
          crcErrorsOption              = parseIntOption((countersTable \\ "td")(4).trimmedText),
          erroredSecondsOption         = parseIntOption((countersTable \\ "td")(10).trimmedText),
          severelyErroredSecondsOption = parseIntOption((countersTable \\ "td")(13).trimmedText)
        ),
        upstream   = RouterStream(
          snrMarginOption              = noneIfDownAndZero(lineUp, parseDoubleOption((dslStatusTable \\ "td")(19).trimmedText)),
          lineAttenuationOption        = noneIfDownAndZero(lineUp, parseDoubleOption((dslStatusTable \\ "td")(16).trimmedText)),
          dataRateOption               = noneIfDownAndZero(lineUp, parseIntOption((dslStatusTable \\ "td")(9).trimmedText)),
          crcErrorsOption              = parseIntOption((countersTable \\ "td")(5).trimmedText),
          erroredSecondsOption         = parseIntOption((countersTable \\ "td")(11).trimmedText),
          severelyErroredSecondsOption = parseIntOption((countersTable \\ "td")(14).trimmedText)
        ),
        //
        // Error counters
        //
        unavailableSecondsOption = None
      )
    }
    log.debug(s"Parsed in $time ms")
    res
  }

  /** If line is down, replace zero value with None */
  def noneIfDownAndZero[N: Numeric](lineUp: Boolean, vo: Option[N]): Option[N] = {
    vo flatMap (v => if (!lineUp && v == 0) None else Some(v))
  }

  private def parseServerIpOption(s: String): Option[String] = {
    notAvailableOr(s, s)
  }

  private def parseModulationOption(s: String): Option[Modulation] = {
    notAvailableOr(s, {
      Modulation.valueOf(s replaceAllLiterally ("+", "PLUS"))
    })
  }

  private def parseDoubleOption(s: String): Option[Double] = {
    notAvailableOr(s, {
      s.toDouble
    })
  }

  private def parseIntOption(s: String): Option[Int] = {
    notAvailableOr(s, {
      s.toInt
    })
  }

  private def notAvailableOr[A](s: String, alternative: => A): Option[A] = {
    if (s == "N/A" || s == "-")
      None
    else
      Some(alternative)
  }
}
