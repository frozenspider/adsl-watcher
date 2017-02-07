package org.fs.rw.utility

trait Imports
    extends com.github.nscala_time.time.Imports
    with org.fs.utility.web.Imports {
  def now = DateTime.now
}

object Imports extends Imports
