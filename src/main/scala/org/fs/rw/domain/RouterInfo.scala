package org.fs.rw.domain

import org.fs.rw.utility.Imports._

/**
 * See http://www.kitz.co.uk/adsl/linestats_explanation.htm
 *
 * @author FS
 */
case class RouterInfo(
  id: Option[Int] = None,
  timestamp: DateTime,
  firmwareOption: Option[String],
  //
  // Connection state
  //
  lineUpOption: Option[Boolean],
  serverIpOption: Option[String],
  /** Type of ADSL Modulation used */
  modulationOption: Option[Modulation],
  annexModeOption: Option[AnnexMode],
  //
  // Connection characteristics
  //
  /** Signal-to-noise ratio margin, in dB */
  snrMarginOption: Option[Double],
  /** Degradation of signal over distance and an indication of the length of a line, in dB */
  lineAttenuationOption: Option[Double],
  /** The speed at which router has synchronised to the DSLAM at the exchange at, in kbps */
  lineRateOption: Option[Int],
  //
  // Error counters
  //
  /** A CRC error indicates that part of the data packet is corrupt and requires retransmission */
  crcErrorsOption: Option[Int],
  /** ES - second periods in which either 1+ coding violations occurred OR at 1+ Loss of Signal events occurred */
  erroredSecondsOption: Option[Int],
  /** SES - second periods which contains 30%+ errored blocks OR several other events e.g. one or more Out Of Frame */
  severelyErroredSecondsOption: Option[Int],
  /** UAS - triggered by 10 consecutive SES, will remove the path from use until 10 consecutive seconds with no SES */
  unavailableSecondsOption: Option[Int]) extends Message
