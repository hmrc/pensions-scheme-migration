/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repositories

import com.google.inject.Inject
import models.cache.{DataJson, MigrationLock}
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

class DataCacheRepository @Inject()(    lockCacheRepository: LockCacheRepository,
                                        mongoComponent: ReactiveMongoComponent,
                                        configuration: Configuration
                                      )(implicit val ec: ExecutionContext)
  extends ReactiveRepository[JsValue, BSONObjectID](
    configuration.get[String](path = "mongodb.migration-cache.data-cache.name"),
    mongoComponent.mongoConnector.db,
    implicitly
  ) {

  private val pstrBson: String => BSONDocument = pstr => BSONDocument("pstr" -> pstr)

  override val logger: Logger = LoggerFactory.getLogger("DataCacheRepository")

  private def expireInSeconds: DateTime = DateTime.now(DateTimeZone.UTC)
    .toLocalDate
    .plusDays(configuration.get[Int](path = "mongodb.migration-cache.data-cache.timeToLiveInDays") + 1)
    .toDateTimeAtStartOfDay()

  override lazy val indexes = Seq(
    Index(Seq(("pstr", Ascending)), Some("pstr"), unique = true),
    Index(Seq(("expireAt", IndexType.Ascending)), Some("dataExpiry"), options = BSONDocument("expireAfterSeconds" -> 0))
  )

  (for { _ <- ensureIndexes } yield { () }) recoverWith {
    case t: Throwable => Future.successful(logger.error(s"Error creating indexes on collection ${collection.name}", t))
  }

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] =
    Future.sequence(indexes.map(collection.indexesManager.ensure(_)))

  def renewLockAndSave(lock: MigrationLock, userData: JsValue)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.debug("Calling Save in Migration Data Cache")

    val lockAndSave: Future[Boolean] = lockCacheRepository.setLock(lock).flatMap {
      case true =>
        val document: JsValue = Json.toJson(DataJson(lock.pstr, userData, DateTime.now(DateTimeZone.UTC), expireInSeconds))

        val modifier = BSONDocument("$set" -> document)
        collection.update.one(pstrBson(lock.pstr), modifier, upsert = true).map(_.ok)
      case _ => Future.failed(LockCouldNotBeSetException)
    }

    lockCacheRepository.getLockByPstr(lock.pstr).flatMap {
      case None => lockAndSave
      case Some(migrationLock) if migrationLock == lock => lockAndSave
      case _ => Future.failed(AlreadyLockedException)
    }
  }

  def get(pstr: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    logger.debug("Calling get in Migration Data Cache")
    collection.find(BSONDocument("pstr" -> pstr),
      projection = Option.empty[JsObject]).one[DataJson].map{ dataJsonOpt =>
      dataJsonOpt.map{dataJson =>
        dataJson.data.as[JsObject] ++
        Json.obj("expireAt" -> JsNumber(dataJson.expireAt.minusDays(1).getMillis))
      }
    }
  }

  def remove(pstr: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.warn(s"Removing row from collection ${collection.name} pstr: $pstr")
    val selector = BSONDocument("pstr" -> pstr)
    collection.delete.one(selector).flatMap { deletion =>
      lockCacheRepository.releaseLockByPstr(pstr).map(_ => deletion.ok)
    }
  }

  case object LockCouldNotBeSetException extends Exception("Lock could not be acquired. Needs further investigation")
  case object AlreadyLockedException extends Exception("Lock has been placed by another user before this save")

}
