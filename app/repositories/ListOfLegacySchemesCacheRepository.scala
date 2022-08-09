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
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model._
import play.api.libs.json.{Format, JsObject, JsValue}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

class ListOfLegacySchemesCacheRepository@Inject()(
                                        mongoComponent: MongoComponent,
                                        configuration: Configuration
                                      )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[JsObject](
    collectionName = configuration.get[String](path = "mongodb.migration-cache.list-of-legacy-schemes.name"),
    mongoComponent = mongoComponent,
    domainFormat = implicitly,
    indexes = Seq(
      IndexModel(
        keys = Indexes.ascending("lastUpdated"),
        indexOptions = IndexOptions().name("lastUpdated")
          .expireAfter(configuration.get[Int](path = "mongodb.migration-cache.list-of-legacy-schemes.timeToLiveInSeconds"),
            TimeUnit.SECONDS)
      )
    )
  ) with Logging {

  implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat

  import ListOfLegacySchemesCacheRepository._

  // scalastyle:on magic.number
//  private case class JsonDataEntry(
//                                    id: String,
//                                    data: JsValue,
//                                    lastUpdated: DateTime
//                                  )

  def upsert(id: String, data: JsValue)(implicit ec: ExecutionContext): Future[Boolean] = {
    val upsertOptions = new FindOneAndUpdateOptions().upsert(true)

    collection.findOneAndUpdate(
      filter = Filters.eq(idKey, id),
      update = Updates.combine(
        set(idKey, id),
        set(dataKey, Codecs.toBson(data)),
        set(lastUpdatedKey, Codecs.toBson(DateTime.now(DateTimeZone.UTC)))
      ),
      upsertOptions
    ).toFuture().map(_ => true)
  }

  /*
    def get(pstr: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    logger.debug("Calling get in Migration Data Cache")
    collection.find(
      filter = Filters.eq(pstrKey, pstr)
    ).toFuture()
      .map(_.headOption)
      .map {
        _.map { dataJson =>
          dataJson.data.as[JsObject] ++
            Json.obj("expireAt" -> JsNumber(dataJson.expireAt.minusDays(1).getMillis))
        }
      }
  }
   */

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    collection.find(
      filter = Filters.eq(idKey, id)
    ).toFuture()
      .map(_.headOption)
      .map { _.flatMap { dataJson => (dataJson \ "data").asOpt[JsObject]}}
//    collection.find(BSONDocument("id" -> id), projection = Option.empty[JsObject]).one[JsonDataEntry].map {
//      _.map {
//        dataEntry =>
//          dataEntry.data
//      }
//    }
  }

  def getLastUpdated(id: String)(implicit ec: ExecutionContext): Future[Option[DateTime]] = {
    collection.find(
      filter = Filters.eq(idKey, id)
    ).toFuture()
      .map(_.headOption)
      .map { _.flatMap { dataJson => (dataJson \ "lastUpdated").asOpt[DateTime]}}
//    collection.find(BSONDocument("id" -> id), projection = Option.empty[JsObject]).one[JsonDataEntry].map {
//      _.map {
//        dataEntry =>
//          dataEntry.lastUpdated
//      }
//    }
  }

  def remove(id: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.warn(s"Removing row from list of legacy schemes collection externalId:$id")
    collection.deleteOne(
      filter = Filters.eq(idKey, id)
    ).toFuture().map( _ => true)
//    val selector = BSONDocument("id" -> id)
//    collection.delete().one(selector).map(_.ok)
  }

}

object ListOfLegacySchemesCacheRepository {
  private val idKey = "id"
  private val dataKey = "data"
  private val lastUpdatedKey = "lastUpdated"
}