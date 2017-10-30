package org.fs.rw.database

import org.fs.rw.domain._
import org.fs.rw.utility.Imports._
import org.fs.utility.StopWatch
import org.flywaydb.core.Flyway
import org.joda.time.DateTime
import org.slf4s.Logging

import com.typesafe.config.Config

import slick.jdbc.meta.MTable
import scala.annotation.tailrec

class SlickDao(config: Config) extends Dao with Logging {

  private val wrapper = new AgnosticDatabaseWrapper("db", config)
  private val database = wrapper.database

  import wrapper.api._
  import Mapping._

  private val routerInfoRecords = TableQuery[RouterInfoRecords]
  private val detectionErrors = TableQuery[DetectionErrors]

  override def setup(): Unit = {
    log.info("Running migrations")
    StopWatch.measureAndCall({
      val flyway = new Flyway
      val source = database.source.asInstanceOf[slick.jdbc.DataSourceJdbcDataSource]
      flyway.setDataSource(source.ds)
      flyway.setBaselineVersionAsString("1")
      // If schema exists - mark it with a baseline version "1"
      flyway.setBaselineOnMigrate(true)
      flyway.migrate()
    })((_, t) => log.info(s"Done in $t ms"))
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

  override def thinOut(cutoffTimeMs: Long, intendedGapMs: Long): Unit = {
    // Code below is fragile and far from being typesafe
    val cutoffTimeMcs = BigInt(cutoffTimeMs) * 1000
    val intendedGapMcs = BigInt(intendedGapMs) * 1000
    for {
      tableName <- Seq(routerInfoRecords, detectionErrors).map(_.baseTableRow.tableName)
    } {
      // Select minimal ID of a close proximity record
      val minIdSql: DBIO[Seq[Option[Long]]] = sql"""
        |SELECT MIN(t.id) as min_id FROM (
        |  SELECT
        |    curr.id,
        |    curr.timestamp AS curr_ts,
        |    prev.timestamp AS prev_ts,
        |    TIMESTAMPDIFF(MICROSECOND, prev.timestamp, curr.timestamp) AS diff
        |  FROM `#${tableName}` curr
        |  INNER JOIN `#${tableName}` prev ON prev.id = curr.id - 1
        |  WHERE TIMESTAMPDIFF(MICROSECOND, curr.timestamp, NOW()) > #${cutoffTimeMcs.toString}
        |  HAVING diff BETWEEN 1 AND #${(intendedGapMcs / 2).toString}
        |) t
        """.stripMargin.as[Option[Long]]
      val minIdSeq = minIdSql.exec().flatten
      minIdSeq.headOption match {
        case Some(minId) if minId > 1 =>
          // Programmatically scroll through the records, copying IDs for removal
          // Start one record earlier than minId to use it as a baseline
          val listRecordsSql: DBIO[Seq[(Long, Long)]] = sql"""
            |SELECT curr.id, UNIX_TIMESTAMP(curr.timestamp) * 1000 AS timestampMs
            |FROM `#${tableName}` curr
            |WHERE curr.id >= #${(minId - 1).toString}
            |  AND TIMESTAMPDIFF(MICROSECOND, curr.timestamp, NOW()) > #${cutoffTimeMcs.toString}
            |ORDER BY curr.id
            """.stripMargin.as[(Long, Long)]
          val records = listRecordsSql.exec()
          def areTooClose(ts1: Long, ts2: Long): Boolean = {
            math.abs(ts1 - ts2) < (intendedGapMs * 0.8)
          }
          @tailrec
          def recurse(referencePoint: Long, records: Seq[(Long, Long)], acc: Seq[Long]): Seq[Long] =
            records match {
              case (id, timestamp) +: tail if areTooClose(timestamp, referencePoint) =>
                recurse(referencePoint, tail, acc :+ id)
              case (id, timestamp) +: tail =>
                recurse(timestamp, tail, acc)
              case _ if records.isEmpty =>
                acc
            }
          val idsToRemove = recurse(records.head._2, records.tail, Seq.empty[Long])
          if (idsToRemove.size > 500) {
            log.info(s"Removing ${idsToRemove.size} old detailed records from ${tableName}")
          }
          // Remove deduced IDs
          if (!idsToRemove.isEmpty) {
            val removeSql = sqlu"DELETE FROM `#${tableName}` WHERE id IN (#${idsToRemove.mkString(",")})"
            removeSql.exec()
          }
        case _ => // NOOP
      }
    }
  }

  override def tearDown(): Unit = {
    database.close()
  }

  /** Action execution helper */
  private implicit class RichAction[+R](a: wrapper.api.DBIOAction[R, wrapper.api.NoStream, Nothing]) {
    import scala.concurrent._
    import scala.concurrent.duration._

    def exec(): R = {
      Await.result(database.run(a), Duration(20, SECONDS))
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

    case class RouterStreamColumns(
      snrMarginOption:              Rep[Option[Double]],
      lineAttenuationOption:        Rep[Option[Double]],
      dataRateOption:               Rep[Option[Int]],
      crcErrorsOption:              Rep[Option[Int]],
      erroredSecondsOption:         Rep[Option[Int]],
      severelyErroredSecondsOption: Rep[Option[Int]]
    )

    implicit object RouterStreamShape extends CaseClassShape(RouterStreamColumns.tupled, RouterStream.tupled)

    abstract class BaseTable[T](tag: Tag, tableName: String) extends Table[T](tag, tableName) {
      def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
      def timestamp = column[DateTime]("timestamp")
    }

    class RouterInfoRecords(tag: Tag) extends BaseTable[RouterInfo](tag, "router_info_records") {
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
      def downstreamSnrMarginOption = column[Option[Double]]("downstream_snr_margin")
      def downstreamLineAttenuationOption = column[Option[Double]]("downstream_line_attenuation")
      def downstreamDataRateOption = column[Option[Int]]("downstream_data_rate")
      def downstreamCrcErrorsOption = column[Option[Int]]("downstream_crc_errors")
      def downstreamErroredSecondsOption = column[Option[Int]]("downstream_errored_seconds")
      def downstreamSeverelyErroredSecondsOption = column[Option[Int]]("downstream_severely_errored_seconds")
      def upstreamSnrMarginOption = column[Option[Double]]("upstream_snr_margin")
      def upstreamLineAttenuationOption = column[Option[Double]]("upstream_line_attenuation")
      def upstreamDataRateOption = column[Option[Int]]("upstream_data_rate")
      def upstreamCrcErrorsOption = column[Option[Int]]("upstream_crc_errors")
      def upstreamErroredSecondsOption = column[Option[Int]]("upstream_errored_seconds")
      def upstreamSeverelyErroredSecondsOption = column[Option[Int]]("upstream_severely_errored_seconds")
      //
      // Error counters
      //
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
        RouterStreamColumns(
          downstreamSnrMarginOption,
          downstreamLineAttenuationOption,
          downstreamDataRateOption,
          downstreamCrcErrorsOption,
          downstreamErroredSecondsOption,
          downstreamSeverelyErroredSecondsOption
        ),
        RouterStreamColumns(
          upstreamSnrMarginOption,
          upstreamLineAttenuationOption,
          upstreamDataRateOption,
          upstreamCrcErrorsOption,
          upstreamErroredSecondsOption,
          upstreamSeverelyErroredSecondsOption
        ),
        //
        // Error counters
        //
        unavailableSecondsOption
      ) <> (RouterInfo.tupled, RouterInfo.unapply)
    }

    class DetectionErrors(tag: Tag) extends BaseTable[DetectionError](tag, "errors") {
      def message = column[String]("message")
      def * = (id.?, timestamp, message) <> ((DetectionError.apply _).tupled, DetectionError.unapply)
    }
  }
}
