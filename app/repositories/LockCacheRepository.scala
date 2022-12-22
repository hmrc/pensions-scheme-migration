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
import com.mongodb.client.model.FindOneAndUpdateOptions
import models.cache.{LockJson, MigrationLock}
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.MongoCommandException
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model._
import play.api.libs.json._
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LockCacheRepository @Inject()(
                                     mongoComponent: MongoComponent,
                                     configuration: Configuration
                                   )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[LockJson](
    collectionName = configuration.get[String](path = "mongodb.migration-cache.lock-cache.name"),
    mongoComponent = mongoComponent,
    domainFormat = implicitly,
    indexes = Seq(
      IndexModel(
        keys = Indexes.ascending("pstr"),
        indexOptions = IndexOptions().name("pstr_index").unique(true)
      ),
      IndexModel(
        keys = Indexes.ascending("credId"),
        indexOptions = IndexOptions().name("credId_index")
      ),
      IndexModel(
        keys = Indexes.ascending("expireAt"),
        indexOptions = IndexOptions().name("dataExpiry").expireAfter(0, TimeUnit.SECONDS)
      )
    )
  ) with Logging {

  private def expireInSeconds: DateTime = DateTime.now(DateTimeZone.UTC).
    plusSeconds(configuration.get[Int](path = "mongodb.migration-cache.lock-cache.timeToLiveInSeconds"))

  private lazy val documentExistsErrorCode = 11000

  private val pstrKey = "pstr"
  private val credIdKey = "credId"
  private val dataKey = "data"

  private val expireAtKey = "expireAt"
  private val lastUpdatedKey = "lastUpdated"

  def setLock(lock: MigrationLock): Future[Boolean] = {
    implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
    val upsertOptions = new FindOneAndUpdateOptions().upsert(true)

    val data: JsValue = Json.toJson(MigrationLock(lock.pstr, lock.credId, lock.psaId))
    collection.findOneAndUpdate(
      filter = Filters.and(
        Filters.eq(pstrKey, lock.pstr),
        Filters.eq(credIdKey, lock.credId)
      ),
      update = Updates.combine(
        set(pstrKey, Codecs.toBson(lock.pstr)),
        set(credIdKey, Codecs.toBson(lock.credId)),
        set(dataKey, Codecs.toBson(data)),
        set(lastUpdatedKey, Codecs.toBson(DateTime.now(DateTimeZone.UTC))),
        set(expireAtKey, Codecs.toBson(expireInSeconds))
      ),
      upsertOptions
    ).toFuture().map { _ => true }
      .recoverWith {
        case e: MongoCommandException if e.getCode == documentExistsErrorCode =>
          Future.successful(false)
      }
  }

  def getLockByPstr(pstr: String): Future[Option[MigrationLock]] = {
    collection.find(
      filter = Filters.eq(pstrKey, pstr)
    ).toFuture()
      .map(_.headOption)
      .map(_.map(_.data.as[MigrationLock]))
  }

  def getLockByCredId(credId: String): Future[Option[MigrationLock]] = {
    collection.find(
      filter = Filters.eq(credIdKey, credId)
    ).toFuture()
      .map(_.headOption)
      .map(_.map(_.data.as[MigrationLock]))
  }

  def getLock(lock: MigrationLock)(implicit ec: ExecutionContext): Future[Option[MigrationLock]] = {
    logger.info("Getting lock")
    collection.find(
      filter = Filters.and(
        Filters.eq(pstrKey, lock.pstr),
        Filters.eq(credIdKey, lock.credId)
      )
    ).toFuture()
      .map(_.headOption)
      .map(_.map(_.data.as[MigrationLock]))
  }

  def releaseLock(lock: MigrationLock): Future[Boolean] = {
    logger.info(s"Removing all rows with pstr id:${lock.pstr} and credId:${lock.credId}")
    collection.deleteOne(
      filter = Filters.and(
        Filters.eq(pstrKey, lock.pstr),
        Filters.eq(credIdKey, lock.credId)
      )
    ).toFuture().map(_ => true)
  }

  def releaseLockByPstr(pstr: String): Future[Boolean] = {
    logger.info(s"Removing all rows with pstr id:$pstr")
    collection.deleteOne(
      filter = Filters.eq(pstrKey, pstr)
    ).toFuture().map(_ => true)
  }

  def releaseLockByCredId(credId: String): Future[Boolean] = {
    logger.info(s"Removing all rows with credId id:$credId")
    collection.deleteOne(
      filter = Filters.eq(credIdKey, credId)
    ).toFuture().map(_ => true)
  }
}
