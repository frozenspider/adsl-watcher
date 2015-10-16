package org.fs.rw.detection

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.apache.commons.lang3.ClassPathUtils
import java.io.File
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class UpvelUR344AN4GPlusSpec
    extends FlatSpec {

  val instance = UpvelUR344AN4GPlus

  behavior of "Upvel UR344AN4G+ detector"

  // FIXME: Improve tests
  it should "parse the up state" in {
    val content = Source.fromFile(new File(routerFolder, "up.htm")).mkString
    val parsed = instance.parse(content)
    assert(parsed.lineUpOption === Some(true))
  }

  it should "parse the up/disconnected state" in {
    val content = Source.fromFile(new File(routerFolder, "up-disconnected.htm")).mkString
    val parsed = instance.parse(content)
    assert(parsed.lineUpOption === Some(true))
  }

  it should "parse the wait-for-init state" in {
    val content = Source.fromFile(new File(routerFolder, "wait-for-init.htm")).mkString
    val parsed = instance.parse(content)
    assert(parsed.lineUpOption === Some(false))
  }

  val resourcesFolder = new File("src/test/resources")
  val routerFolder = new File(resourcesFolder, "UR-344AN4G+")
}
