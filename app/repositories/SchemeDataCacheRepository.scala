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
import models.cache.DataJson
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

class SchemeDataCacheRepository @Inject()(mongoComponent: ReactiveMongoComponent,
                                          configuration: Configuration
                                      )(implicit val ec: ExecutionContext)
  extends ReactiveRepository[JsValue, BSONObjectID](
    configuration.get[String](path = "mongodb.migration-cache.scheme-data-cache.name"),
    mongoComponent.mongoConnector.db,
    implicitly
  ) {

  override val logger: Logger = LoggerFactory.getLogger("SchemeDataCacheRepository")

  private def expireInSeconds: DateTime = DateTime.now(DateTimeZone.UTC)
    .toLocalDate
    .plusDays(configuration.get[Int](path = "mongodb.migration-cache.scheme-data-cache.timeToLiveInDays") + 1)
    .toDateTimeAtStartOfDay()

  override lazy val indexes = Seq(
    Index(Seq(("id", Ascending)), Some("id"), unique = true),
    Index(Seq(("expireAt", IndexType.Ascending)), Some("dataExpiry"), options = BSONDocument("expireAfterSeconds" -> 0))
  )

  (for { _ <- ensureIndexes } yield { () }) recoverWith {
    case t: Throwable => Future.successful(logger.error(s"Error creating indexes on collection ${collection.name}", t))
  }

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] =
    Future.sequence(indexes.map(collection.indexesManager.ensure(_)))

  def save(id: String, userData: JsValue)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.debug("Calling Save in Scheme Data Cache")

    val document: JsValue = Json.toJson(DataJson(id, userData, DateTime.now(DateTimeZone.UTC), expireInSeconds))
    val modifier = BSONDocument("$set" -> document)

    collection.update.one(BSONDocument("id" -> id), modifier, upsert = true).map(_.ok)
  }

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    logger.debug("Calling get in Scheme Data Cache")
    collection.find(BSONDocument("id" -> id), projection = Option.empty[JsObject]).one[DataJson].map(_.map(_.data))
  }

  def remove(id: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.warn(s"Removing row from collection ${collection.name}")
    val selector = BSONDocument("id" -> id)
    collection.delete.one(selector).map(_.ok)
  }
}

