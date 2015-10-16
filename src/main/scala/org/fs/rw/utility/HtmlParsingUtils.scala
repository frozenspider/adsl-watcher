package org.fs.rw.utility

import scala.xml.XML
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.Node

trait HtmlParsingUtils {
  private val saxFactory = new SAXFactoryImpl()

  def parseElement(bodyString: String): Elem = {
    val xmlParser = XML.withSAXParser(saxFactory.newSAXParser())
    xmlParser.loadString(bodyString)
  }

  implicit class HtmlNodeSeq(ns: NodeSeq) {
    def filterByClass(c: String): NodeSeq =
      ns filter (_.attribute("class") match {
        case Some(Text(t)) => t split " " contains c
        case _             => false
      })
  }

  implicit class HtmlNode(n: Node) {
    def cleanText: String = trim(n.text)
  }

  /** More appropriate version of trim than commons-lang3's StringUtils.strip() */
  private def trim(s: String): String = {
    def isSpace(c: Char) = Character.isWhitespace(c) || Character.isSpaceChar(c)
    s.dropWhile(isSpace).reverse.dropWhile(isSpace).reverse
  }
}
