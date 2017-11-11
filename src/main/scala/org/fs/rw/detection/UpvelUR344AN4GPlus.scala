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

object UpvelUR344AN4GPlus extends Detector {
  /** Section -> Subsection -> RowHeader -> Seq(Values) */
  private type DataMap = Map[String, Map[String, Map[String, Seq[String]]]]

  private sealed trait RowDataType { def s: String }
  private case class TitleSection(s: String) extends RowDataType
  private case class TitleSubsection(s: String) extends RowDataType
  private case class Data(s: String) extends RowDataType

  val timeoutMs = 60 * 1000

  val deviceInfoUri: String = "cgi-bin/status_deviceinfo.asp"

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
      val authRequest = {
        // Funny thing is... router doesn't actually care about auth. Well fkin played, Upvel.
        val req = POST(s"http://$routerIp/$deviceInfoUri").addParameter("DvInfo_PVC", interface).addTimeout(timeoutMs)
        if (rootResponse.code == 200) req else req.addBasicAuth(username, password)
      }
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
    val (res, time) = StopWatch.measure {
      val body = (parseElement(content) \ "body").head
      val dataMap: DataMap = scrapeDataMap(body)
      RouterInfo(
        timestamp      = DateTime.now,
        firmwareOption = Some(dataMap("Device Information")("")("Firmware Version").head),
        //
        // Connection state
        //
        lineUpOption     = Some(dataMap("ADSL")("")("Line State").head == "up"),
        serverIpOption   = parseServerIpOption(dataMap("WAN")("IPv4")("IP Address").head),
        modulationOption = parseModulationOption(dataMap("ADSL")("")("Modulation").head),
        annexModeOption  = parseAnnexOption(dataMap("ADSL")("")("Annex Mode").head),
        //
        // Connection characteristics
        //
        downstream = RouterStream(
          snrMarginOption              = parseDecibellsOption(dataMap("ADSL")("")("SNR Margin").head),
          lineAttenuationOption        = parseDecibellsOption(dataMap("ADSL")("")("Line Attenuation").head),
          dataRateOption               = parseDataRateOption(dataMap("ADSL")("")("Data Rate").head),
          crcErrorsOption              = None,
          erroredSecondsOption         = parseSecondsOption(dataMap("ADSL")("")("ES").head),
          severelyErroredSecondsOption = parseSecondsOption(dataMap("ADSL")("")("SES").head)
        ),
        upstream   = RouterStream(
          snrMarginOption              = parseDecibellsOption(dataMap("ADSL")("")("SNR Margin").last),
          lineAttenuationOption        = parseDecibellsOption(dataMap("ADSL")("")("Line Attenuation").last),
          dataRateOption               = parseDataRateOption(dataMap("ADSL")("")("Data Rate").last),
          crcErrorsOption              = None,
          erroredSecondsOption         = parseSecondsOption(dataMap("ADSL")("")("ES").last),
          severelyErroredSecondsOption = parseSecondsOption(dataMap("ADSL")("")("SES").last)
        ),
        //
        // Error counters
        //
        unavailableSecondsOption = parseSecondsOption(dataMap("ADSL")("")("UAS").head)
      )
    }
    log.debug(s"Parsed in $time ms")
    res
  }

  private def scrapeDataMap(body: Node): DataMap = {
    val dataRows = body \\ "tr"
    val rowsSeq = dataRows map (
      _ \ "td" collect {
        case td if td.classes.contains("title-main")                                        => TitleSection(td.trimmedText)
        case td if td.classes.contains("title-sub")                                         => TitleSubsection(td.trimmedText)
        case td if td.classes.contains("tabdata") && !Seq(":", "").contains(td.trimmedText) => Data(td.trimmedText)
      }
    ) filter (!_.isEmpty)
    val dataMap: DataMap = scrapeDataMapRecursively(rowsSeq, Map.empty, None, None)
    dataMap
  }

  @tailrec
  private def scrapeDataMapRecursively(
      rowsSeq:                 Seq[Seq[RowDataType]],
      dataMapSoFar:            DataMap,
      currentSectionOption:    Option[(String, Map[String, Map[String, Seq[String]]])],
      currentSubsectionOption: Option[(String, Map[String, Seq[String]])]
  ): DataMap = rowsSeq match {
    case Seq(TitleSection(s)) +: rowsSeqTail if currentSectionOption.isEmpty =>
      val currentSection = s -> Map.empty[String, Map[String, Seq[String]]]
      scrapeDataMapRecursively(rowsSeqTail, dataMapSoFar, Some(currentSection), None)
    case Seq(TitleSection(_)) +: _ =>
      val dataMapSoFar2 = appendDataMapSection(dataMapSoFar, currentSectionOption, currentSubsectionOption)
      scrapeDataMapRecursively(rowsSeq, dataMapSoFar2, None, None)
    case Seq(TitleSubsection(s)) +: cellsSeqRest =>
      require(!currentSectionOption.isEmpty, "Data map scraping failed, section is not defined")
      val currentSection = currentSubsectionOption match {
        case Some(currentSubsection) => appendDataMapSubsection(currentSectionOption, currentSubsection)
        case None                    => currentSectionOption.get
      }
      scrapeDataMapRecursively(cellsSeqRest, dataMapSoFar, Some(currentSection), Some(s -> Map.empty))
    case (Data(_) +: _) +: _ if currentSubsectionOption.isEmpty =>
      scrapeDataMapRecursively(rowsSeq, dataMapSoFar, currentSectionOption, Some("" -> Map.empty))
    case (Data(s) +: rowTail) +: rowsSeqTail =>
      val currentSubsection = currentSubsectionOption.get match {
        case (name, cells) => name -> (cells + (s -> rowTail.map(_.s)))
      }
      val dataMapSoFar2 = appendDataMapSection(dataMapSoFar, currentSectionOption, Some(currentSubsection))
      scrapeDataMapRecursively(rowsSeqTail, dataMapSoFar2, currentSectionOption, Some(currentSubsection))
    case nil if nil.isEmpty && currentSubsectionOption.isEmpty =>
      scrapeDataMapRecursively(rowsSeq, dataMapSoFar, currentSectionOption, Some("" -> Map.empty))
    case nil if nil.isEmpty =>
      appendDataMapSection(dataMapSoFar, currentSectionOption, currentSubsectionOption)
  }

  private def appendDataMapSection(
      dataMapSoFar:            DataMap,
      currentSectionOption:    Option[(String, Map[String, Map[String, Seq[String]]])],
      currentSubsectionOption: Option[(String, Map[String, Seq[String]])]
  ): DataMap = {
    val currentSubsection = currentSubsectionOption.get
    val currentSection = appendDataMapSubsection(currentSectionOption, currentSubsection)
    val dataMap = dataMapSoFar + currentSection
    dataMap
  }

  private def appendDataMapSubsection(
      currentSectionOption: Option[(String, Map[String, Map[String, Seq[String]]])],
      currentSubsection:    (String, Map[String, Seq[String]])
  ): (String, Map[String, Map[String, Seq[String]]]) = {
    currentSectionOption match {
      case Some((name, subsections)) => name -> (subsections + currentSubsection)
      case None                      => throw new IllegalStateException(s"Data map scraping failed, section for ${currentSubsection._1} is not defined")
    }
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
