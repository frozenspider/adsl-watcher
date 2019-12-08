package org.fs.rw.detection

import java.io.File

import scala.io.Source

import org.fs.rw.domain.AnnexMode
import org.fs.rw.domain.Modulation
import org.fs.rw.domain.RouterInfo
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TendaD820BSpec
  extends FlatSpec {

  val instance = TendaD820B

  behavior of "Tenda D820B detector, firmware Tenda_EN_B_100326"

  it should "parse the idle state" in {
    val content = Source.fromFile(new File(routerFolder, "b-100326-idle.htm")).mkString
    val parsed = instance.parseContent(content).asInstanceOf[RouterInfo]
    assert(parsed.firmwareOption === Some("Tenda_EN_B_100326"))
    assert(parsed.lineUpOption === Some(false))
    assert(parsed.serverIpOption === None)
    assert(parsed.modulationOption === None)
    assert(parsed.annexModeOption === None)
    assert(parsed.downstream.snrMarginOption === None)
    assert(parsed.downstream.lineAttenuationOption === None)
    assert(parsed.downstream.dataRateOption === None)
    assert(parsed.downstream.crcErrorsOption === Some(0))
    assert(parsed.downstream.erroredSecondsOption === Some(0))
    assert(parsed.downstream.severelyErroredSecondsOption === Some(0))
    assert(parsed.upstream.snrMarginOption === None)
    assert(parsed.upstream.lineAttenuationOption === None)
    assert(parsed.upstream.dataRateOption === None)
    assert(parsed.upstream.crcErrorsOption === Some(7604))
    assert(parsed.upstream.erroredSecondsOption === Some(2490))
    assert(parsed.upstream.severelyErroredSecondsOption === Some(352))
    assert(parsed.unavailableSecondsOption === None)
  }

  it should "parse the up state" in {
    val content = Source.fromFile(new File(routerFolder, "b-100326-up.htm")).mkString
    val parsed = instance.parseContent(content).asInstanceOf[RouterInfo]
    assert(parsed.firmwareOption === Some("Tenda_EN_B_100326"))
    assert(parsed.lineUpOption === Some(true))
    assert(parsed.serverIpOption === None)
    assert(parsed.modulationOption === Some(Modulation.ADSL2PLUS))
    assert(parsed.annexModeOption === None)
    assert(parsed.downstream.snrMarginOption === Some(10.5))
    assert(parsed.downstream.lineAttenuationOption === Some(22.0))
    assert(parsed.downstream.dataRateOption === Some(2044))
    assert(parsed.downstream.crcErrorsOption === Some(0))
    assert(parsed.downstream.erroredSecondsOption === Some(0))
    assert(parsed.downstream.severelyErroredSecondsOption === Some(0))
    assert(parsed.upstream.snrMarginOption === Some(5.5))
    assert(parsed.upstream.lineAttenuationOption === Some(10.5))
    assert(parsed.upstream.dataRateOption === Some(596))
    assert(parsed.upstream.crcErrorsOption === Some(7604))
    assert(parsed.upstream.erroredSecondsOption === Some(2490))
    assert(parsed.upstream.severelyErroredSecondsOption === Some(352))
    assert(parsed.unavailableSecondsOption === None)
  }

  it should "parse the G.dmt (ADSL1) modulation" in {
    val content = Source.fromFile(new File(routerFolder, "b-100326-modulation-g.dmt.htm")).mkString
    val parsed = instance.parseContent(content).asInstanceOf[RouterInfo]
    assert(parsed.firmwareOption === Some("Tenda_EN_B_100326"))
    assert(parsed.lineUpOption === Some(true))
    assert(parsed.serverIpOption === None)
    assert(parsed.modulationOption === Some(Modulation.ADSL1))
    assert(parsed.annexModeOption === None)
    assert(parsed.downstream.snrMarginOption === Some(11.5))
    assert(parsed.downstream.lineAttenuationOption === Some(22.5))
    assert(parsed.downstream.dataRateOption === Some(2048))
    assert(parsed.downstream.crcErrorsOption === Some(22220))
    assert(parsed.downstream.erroredSecondsOption === None)
    assert(parsed.downstream.severelyErroredSecondsOption === None)
    assert(parsed.upstream.snrMarginOption === Some(8.0))
    assert(parsed.upstream.lineAttenuationOption === Some(10.0))
    assert(parsed.upstream.dataRateOption === Some(608))
    assert(parsed.upstream.crcErrorsOption === Some(22221))
    assert(parsed.upstream.erroredSecondsOption === None)
    assert(parsed.upstream.severelyErroredSecondsOption === None)
    assert(parsed.unavailableSecondsOption === None)
  }

  val resourcesFolder = new File("src/test/resources")
  val routerFolder = new File(resourcesFolder, "tenda-d820b")
}
