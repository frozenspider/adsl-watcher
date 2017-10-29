package org.fs.rw.domain

/**
 * Upstream and downstream connection characteristics
 *
 * @author FS
 */
case class RouterStream(
  /** Signal-to-noise ratio margin, in dB */
  snrMarginOption: Option[Double],
  /** Degradation of signal over distance and an indication of the length of a line, in dB */
  lineAttenuationOption: Option[Double],
  /** The speed at which router has synchronised to the DSLAM at the exchange at, in kbps */
  dataRateOption: Option[Int],
  /** A CRC error indicates that part of the data packet is corrupt and requires retransmission */
  crcErrorsOption: Option[Int],
  /** ES - second periods in which either 1+ coding violations occurred OR at 1+ Loss of Signal events occurred */
  erroredSecondsOption: Option[Int],
  /** SES - second periods which contains 30%+ errored blocks OR several other events e.g. one or more Out Of Frame */
  severelyErroredSecondsOption: Option[Int]
)
