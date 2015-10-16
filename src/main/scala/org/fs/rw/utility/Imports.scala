package org.fs.rw.utility

trait Imports
    extends com.github.nscala_time.time.Imports
    with HtmlParsingUtils {
  def now = DateTime.now
}

object Imports extends Imports
