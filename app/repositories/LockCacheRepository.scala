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
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.LastError
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import models.cache.{LockJson, MigrationLock}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

class LockCacheRepository @Inject()(
                                        mongoComponent: ReactiveMongoComponent,
                                        configuration: Configuration
                                      )(implicit val ec: ExecutionContext)
  extends ReactiveRepository[JsValue, BSONObjectID](
    configuration.get[String](path = "mongodb.migration-cache.lock-cache.name"),
    mongoComponent.mongoConnector.db,
    implicitly
  ) {

  override val logger: Logger = LoggerFactory.getLogger("LockCacheRepository")

  private def expireInSeconds: DateTime = DateTime.now(DateTimeZone.UTC).
    plusSeconds(configuration.get[Int](path = "mongodb.migration-cache.lock-cache.timeToLiveInSeconds"))

  override lazy val indexes: Seq[Index] = Seq(
    Index(key = Seq("pstr" -> Ascending), name = Some("pstr_index"), unique = true),
    Index(key = Seq("credId" -> Ascending), name = Some("credId_index")),
    Index(key = Seq("expireAt" -> Ascending), name = Some("dataExpiry"), options = BSONDocument("expireAfterSeconds" -> 0))
  )

  (for { _ <- ensureIndexes } yield { () }) recoverWith {
    case t: Throwable => Future.successful(logger.error(s"Error creating indexes on collection ${collection.name}", t))
  } andThen {
    case _ => CollectionDiagnostics.logCollectionInfo(collection)
  }

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] =
    Future.sequence(indexes.map(collection.indexesManager.ensure(_)))

  private val lockBson: MigrationLock => BSONDocument = lock =>
    BSONDocument("pstr" -> lock.pstr, "credId" -> lock.credId)

  private def pstrBson: String => BSONDocument = pstr => BSONDocument("pstr" -> pstr)

  private def credIdBson: String => BSONDocument = credId => BSONDocument("credId" -> credId)

  private val modifier: MigrationLock => BSONDocument = lock =>
    BSONDocument("$set" -> Json.toJson(
      LockJson(lock.pstr, lock.credId, Json.toJson(lock), DateTime.now(DateTimeZone.UTC), expireInSeconds)))

  private lazy val documentExistsErrorCode = Some(11000)

  def setLock(lock: MigrationLock): Future[Boolean] =
    collection.update(true).one(lockBson(lock), modifier(lock), upsert = true).map(_.ok)  recoverWith {
      case e: LastError if e.code == documentExistsErrorCode =>
        Future(false)
    }

  def getLockByPstr(pstr: String): Future[Option[MigrationLock]] = {
    collection.find(pstrBson(pstr), Option.empty[JsObject]).one[LockJson].map(_.map(_.data.as[MigrationLock]))
  }

  def getLockByCredId(credId: String): Future[Option[MigrationLock]] =
    collection.find(credIdBson(credId), Option.empty[JsObject]).one[LockJson].map(_.map(_.data.as[MigrationLock]))

  def getLock(lock: MigrationLock)(implicit ec: ExecutionContext): Future[Option[MigrationLock]] = {
    logger.info("Getting lock")
    collection.find(lockBson(lock), Option.empty[JsObject]).one[LockJson].map(_.map(_.data.as[MigrationLock]))
  }

  def releaseLock(lock: MigrationLock): Future[Boolean] = {
    logger.info(s"Removing all rows with pstr id:${lock.pstr} and credId:${lock.credId}")
    collection.delete.one(lockBson(lock)).map(_.ok)
  }

  def releaseLockByPstr(pstr: String): Future[Boolean] = {
    logger.info(s"Removing all rows with pstr id:$pstr")
    collection.delete.one(pstrBson(pstr)).map(_.ok)
  }

  def releaseLockByCredId(credId: String): Future[Boolean] = {
    logger.info(s"Removing all rows with credId id:$credId")
    collection.delete.one(credIdBson(credId)).map(_.ok)
  }

}
