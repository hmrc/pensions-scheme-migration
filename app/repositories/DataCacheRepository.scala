/*
 * Copyright 2023 HM Revenue & Customs
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
import crypto.DataEncryptor
import models.cache.{DataJson, MigrationLock}
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model._
import play.api.libs.json._
import play.api.{Configuration, Logging}
import repositories.DataCacheRepository.{AlreadyLockedException, LockCouldNotBeSetException}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import org.mongodb.scala.gridfs.ObservableFuture


import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneId}
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataCacheRepository @Inject()(
                                     lockCacheRepository: LockCacheRepository,
                                     mongoComponent: MongoComponent,
                                     configuration: Configuration,
                                     cipher: DataEncryptor
                                   )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[DataJson](
    collectionName = configuration.get[String](path = "mongodb.migration-cache.data-cache.name"),
    mongoComponent = mongoComponent,
    domainFormat = implicitly,
    indexes = Seq(
      IndexModel(
        keys = Indexes.ascending("pstr"),
        indexOptions = IndexOptions().name("pstr").unique(true).background(true)
      ),
      IndexModel(
        keys = Indexes.ascending("expireAt"),
        indexOptions = IndexOptions().name("dataExpiry").expireAfter(0, TimeUnit.SECONDS)
      )
    )
  ) with Logging {

  private val pstrKey = "pstr"
  private val dataKey = "data"
  private val expireAtKey = "expireAt"
  private val lastUpdatedKey = "lastUpdated"

  private def expireInSeconds: Instant = LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant
    .plus(configuration.get[Int](path = "mongodb.migration-cache.data-cache.timeToLiveInDays") + 1, ChronoUnit.DAYS)

  def renewLockAndSave(lock: MigrationLock, userData: JsValue)(implicit ec: ExecutionContext): Future[Boolean] = {

    val upsertOptions = new FindOneAndUpdateOptions().upsert(true)
    logger.debug("Calling Save in Migration Data Cache")

    val lockAndSave: Future[Boolean] = lockCacheRepository.setLock(lock).flatMap {
      case true =>
        collection.findOneAndUpdate(
          filter = Filters.eq(pstrKey, lock.pstr),
          update = Updates.combine(
            set(pstrKey, Codecs.toBson(lock.pstr)),
            set(dataKey, Codecs.toBson(cipher.encrypt(lock.pstr, userData))),
            set(lastUpdatedKey, Instant.now()),
            set(expireAtKey, expireInSeconds)
          ),
          upsertOptions
        ).toFuture().map(_ => true)
      case _ => Future.failed(LockCouldNotBeSetException)
    }

    lockCacheRepository.getLockByPstr(lock.pstr).flatMap {
      case None =>
        lockAndSave
      case Some(migrationLock) if migrationLock == lock =>
        lockAndSave
      case _ =>
        Future.failed(AlreadyLockedException)
    }
  }

  def get(pstr: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    logger.debug("Calling get in Migration Data Cache")
    collection.find(
      filter = Filters.eq(pstrKey, pstr)
    ).toFuture()
      .map(_.headOption)
      .map {
        _.map { dataJson =>
          cipher.decrypt(pstr, dataJson.data).as[JsObject] ++
            Json.obj("expireAt" -> JsNumber(dataJson.expireAt.minus(1, ChronoUnit.DAYS).toEpochMilli))
        }
      }
  }

  def remove(pstr: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.info(s"Removing row from data cache collection - pstr: $pstr")
    collection.deleteOne(
      filter = Filters.eq(pstrKey, pstr)
    ).toFuture().flatMap { _ =>
      lockCacheRepository.releaseLockByPstr(pstr).map(_ => true)

    }
  }

  def saveMigratedData(pstr: String, data: JsValue): Future[Boolean] = {
    logger.debug("Calling saveMigratedData in Migration Data Cache")

    val upsertOptions = new FindOneAndUpdateOptions().upsert(true)

    collection.findOneAndUpdate(
      filter = Filters.eq(pstrKey, pstr),
      update = Updates.combine(
        set(pstrKey, pstr),
        set(dataKey, Codecs.toBson(cipher.encrypt(pstr, data))),
        set(lastUpdatedKey, Instant.now()),
        set(expireAtKey, expireInSeconds)
      ),
      upsertOptions
    ).toFuture().map(_ => true)
  }

}

object DataCacheRepository {
  case object LockCouldNotBeSetException extends Exception("Lock could not be acquired. Needs further investigation")
  case object AlreadyLockedException extends Exception("Lock has been placed by another user before this save")
}
