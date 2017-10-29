package org.fs.rw.domain

import org.fs.rw.utility.Imports._

/**
 * See http://www.kitz.co.uk/adsl/linestats_explanation.htm
 *
 * @author FS
 */
case class RouterInfo(
  id:                     Option[Int]    = None,
  override val timestamp: DateTime,
  firmwareOption:         Option[String],
  //
  // Connection state
  //
  lineUpOption:   Option[Boolean],
  serverIpOption: Option[String],
  /** Type of ADSL Modulation used */
  modulationOption: Option[Modulation],
  annexModeOption:  Option[AnnexMode],
  //
  // Connection characteristics
  //
  downstream: RouterStream,
  upstream:   RouterStream,
  //
  // Error counters
  //
  /** UAS - triggered by 10 consecutive SES, will remove the path from use until 10 consecutive seconds with no SES */
  unavailableSecondsOption: Option[Int]
) extends Message
