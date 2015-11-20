package org.fs.rw.database

import org.fs.rw.domain._
import org.joda.time.DateTime
import org.slf4s.Logging

import com.typesafe.config.Config

import slick.dbio._
import slick.jdbc.meta.MTable

class SlickDao(config: Config) extends Dao with Logging {

  private val wrapper = new AgnosticDatabaseWrapper("db", config)
  private val database = wrapper.database

  import wrapper.api._
  import Mapping._

  private val routerInfoRecords = TableQuery[RouterInfoRecords]
  private val detectionErrors = TableQuery[DetectionErrors]

  override def setup(): Unit = {
    if (MTable.getTables.exec().isEmpty) {
      (routerInfoRecords.schema ++ detectionErrors.schema).create.exec()
      log.info("Schema initialized")
    } else {
      log.info("Schema exists")
    }
  }

  override def saveMessage(message: Message): Unit = {
    val action = message match {
      case m: DetectionError =>
        detectionErrors += m
      case m: RouterInfo =>
        routerInfoRecords += m
    }
    action.exec()
    log.debug("Message saved")
  }

  override def tearDown(): Unit = {
    database.close()
  }


  /** Action execution helper */
  private implicit class RichAction[+R](a: wrapper.api.DBIOAction[R, _ <: NoStream, _ <: Effect]) {
    import scala.concurrent._
    import scala.concurrent.duration._

    def exec(): R = {
      Await.result(database.run(a), Duration(10, SECONDS))
    }
  }

  /** Slick mapping */
  private object Mapping {
    implicit val DateTimeMapper =
      MappedColumnType.base[DateTime, java.sql.Timestamp] (
        v => new java.sql.Timestamp(v.getMillis),
        v => new DateTime(v.getTime)
      )

    implicit val ModulationMapper =
      MappedColumnType.base[Modulation, String] (
        v => v.toString,
        v => Modulation.valueOf(v)
      )

    implicit val AnnexModeMapper =
      MappedColumnType.base[AnnexMode, String] (
        v => v.toString,
        v => AnnexMode.valueOf(v)
      )

    class RouterInfoRecords(tag: Tag) extends Table[RouterInfo](tag, "router_info_records") {
      def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
      def timestamp = column[DateTime]("timestamp")

      def firmwareOption = column[Option[String]]("firmware")
      //
      // Connection state
      //
      def lineUpOption = column[Option[Boolean]]("line_up")
      def serverIpOption = column[Option[String]]("server_ip")
      def modulationOption = column[Option[Modulation]]("modulation")
      def annexModeOption = column[Option[AnnexMode]]("annex_mode")
      //
      // Connection characteristics
      //
      def snrMarginOption = column[Option[Double]]("snr_margin")
      def lineAttenuationOption = column[Option[Double]]("line_attenuation")
      def lineRateOption = column[Option[Int]]("line_rate")
      //
      // Error counters
      //
      def crcErrorsOption = column[Option[Int]]("crc_errors")
      def erroredSecondsOption = column[Option[Int]]("errored_seconds")
      def severelyErroredSecondsOption = column[Option[Int]]("severely_errored_seconds")
      def unavailableSecondsOption = column[Option[Int]]("unavailable_seconds")

      def * = (
        id.?,
        timestamp,
        firmwareOption,
        //
        // Connection state
        //
        lineUpOption,
        serverIpOption,
        modulationOption,
        annexModeOption,
        //
        // Connection characteristics
        //
        snrMarginOption,
        lineAttenuationOption,
        lineRateOption,
        //
        // Error counters
        //
        crcErrorsOption,
        erroredSecondsOption,
        severelyErroredSecondsOption,
        unavailableSecondsOption
      ) <> (RouterInfo.tupled, RouterInfo.unapply)
    }

    class DetectionErrors(tag: Tag) extends Table[DetectionError](tag, "errors") {
      def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
      def timestamp = column[DateTime]("timestamp")
      def message = column[String]("message")
      def * = (id.?, timestamp, message) <> ((DetectionError.apply _).tupled, DetectionError.unapply)
    }
  }
}
