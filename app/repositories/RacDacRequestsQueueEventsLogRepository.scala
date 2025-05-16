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
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model._
import play.api.libs.json._
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import org.mongodb.scala.gridfs.ObservableFuture

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RacDacRequestsQueueEventsLogRepository @Inject()(mongoComponent: MongoComponent,
                                                       configuration: Configuration
                                                      )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[JsObject](
    collectionName = configuration.get[String](path = "mongodb.migration-cache.rac-dac-requests-queue-events-log.name"),
    mongoComponent = mongoComponent,
    domainFormat = implicitly,
    indexes = Seq(
      IndexModel(
        keys = Indexes.ascending("id"),
        indexOptions = IndexOptions().name("id").unique(true).background(true)
      ),
      IndexModel(
        keys = Indexes.ascending("expireAt"),
        indexOptions = IndexOptions().name("dataExpiry")
          .expireAfter(0, TimeUnit.SECONDS)
      )
    )
  ) with Logging {

  import RacDacRequestsQueueEventsLogRepository._

  private def expireInSeconds: Instant = LocalDateTime.now(ZoneId.of("UTC")).toInstant(ZoneOffset.UTC)
    .plus(configuration.get[Int](path = "mongodb.migration-cache.rac-dac-requests-queue-events-log.timeToLiveInSeconds"), ChronoUnit.SECONDS)

  def save(id: String, userData: JsValue)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.debug("Calling Save in RacDacRequestsQueueEventsLogRepository")
    val upsertOptions = new FindOneAndUpdateOptions().upsert(true)

    collection.findOneAndUpdate(
      filter = Filters.eq(idKey, id),
      update = Updates.combine(
        set(idKey, id),
        set(dataKey, Codecs.toBson(userData)),
        set(lastUpdatedKey, Instant.now()),
        set(expireAtKey, expireInSeconds)
      ),
      upsertOptions
    ).toFuture().map(_ => true)
  }

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    logger.debug("Calling get in RacDacRequestsQueueEventsLogRepository")

    collection.find(
      filter = Filters.eq(idKey, id)
    ).toFuture()
      .map(_.headOption)
      .map {
        _.flatMap { dataJson => (dataJson \ "data").asOpt[JsObject] }
      }
  }

  def remove(id: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.info(s"Removing row from collection rac dac requests queue events log repository")
    collection.deleteOne(
      filter = Filters.eq(idKey, id)
    ).toFuture().map(_ => true)
  }
}

object RacDacRequestsQueueEventsLogRepository {
  private val idKey = "id"
  private val dataKey = "data"
  private val lastUpdatedKey = "lastUpdated"
  private val expireAtKey = "expireAt"
}