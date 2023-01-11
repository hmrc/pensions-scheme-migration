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

import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.{Format, JsObject, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class RacDacRequestsQueueEventsLogRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with EmbeddedMongoDBSupport with BeforeAndAfter with
  BeforeAndAfterAll with BeforeAndAfterEach with ScalaFutures { // scalastyle:off magic.number

  import RacDacRequestsQueueEventsLogRepositorySpec._

  var racDacRequestsQueueEventsLogRepository: RacDacRequestsQueueEventsLogRepository = _

  override def beforeAll(): Unit = {
    when(mockConfiguration.get[String](ArgumentMatchers.eq("mongodb.migration-cache.rac-dac-requests-queue-events-log.name"))(ArgumentMatchers.any()))
      .thenReturn("rac-dac-requests-queue-events-log")
    when(mockConfiguration.get[Int](ArgumentMatchers.eq("mongodb.migration-cache.rac-dac-requests-queue-events-log.timeToLiveInSeconds"))
      (ArgumentMatchers.any()))
      .thenReturn(3600)

    initMongoDExecutable()
    startMongoD()
    racDacRequestsQueueEventsLogRepository = buildFormRepository(mongoHost, mongoPort)
    super.beforeAll()
  }

  override def afterAll(): Unit =
    stopMongoD()

  override def beforeEach(): Unit = {
    reset(mockConfiguration)
    super.beforeEach()
  }

  "get" must {
    "get data from Mongo collection when present" in {

      val result = for {
        _ <- racDacRequestsQueueEventsLogRepository.collection.drop().toFuture()
        _ <- racDacRequestsQueueEventsLogRepository.collection.insertMany(seqExistingData).toFuture()
        status <- racDacRequestsQueueEventsLogRepository.get(id1)
      } yield {
        status
      }

      Await.result(result, Duration.Inf) match {
        case status =>
          status mustBe Some(data1)
      }
    }

    "get None from Mongo collection when not present" in {

      val result = for {
        _ <- racDacRequestsQueueEventsLogRepository.collection.drop().toFuture()
        _ <- racDacRequestsQueueEventsLogRepository.collection.insertMany(seqExistingData).toFuture()
        status <- racDacRequestsQueueEventsLogRepository.get("dummyId")
      } yield {
        status
      }

      Await.result(result, Duration.Inf) match {
        case status =>
          status mustBe None
      }
    }
  }


  "save" must {
    "insert into Mongo collection where item does not exist" in {

      val result = for {
        _ <- racDacRequestsQueueEventsLogRepository.collection.drop().toFuture()
        status <- racDacRequestsQueueEventsLogRepository.save(id2, data2)
        allDocs <- racDacRequestsQueueEventsLogRepository.collection.find().toFuture()
      } yield {
        Tuple2(allDocs.size, status)
      }

      Await.result(result, Duration.Inf) match {
        case Tuple2(totalDocs, status) =>
          status mustBe true
          totalDocs mustBe 1
      }
    }

    "update Mongo collection where item does exist" in {

      val result = for {
        _ <- racDacRequestsQueueEventsLogRepository.collection.drop().toFuture()
        _ <- racDacRequestsQueueEventsLogRepository.collection.insertMany(seqExistingData).toFuture()
        _ <- racDacRequestsQueueEventsLogRepository.save(id2, data1)
        updatedItem <- racDacRequestsQueueEventsLogRepository.get(id2)
      } yield {
        updatedItem
      }

      Await.result(result, Duration.Inf) match {
        case updatedItem =>
          updatedItem mustBe Some(data1)
      }
    }
  }

  "remove" must {
    "remove from Mongo collection leaving other one" in {

      val endState = for {
        _ <- racDacRequestsQueueEventsLogRepository.collection.drop().toFuture()
        _ <- racDacRequestsQueueEventsLogRepository.collection.insertMany(seqExistingData).toFuture()
        response <- racDacRequestsQueueEventsLogRepository.remove(id2)
        firstRetrieved <- racDacRequestsQueueEventsLogRepository.get(id1)
        secondRetrieved <- racDacRequestsQueueEventsLogRepository.get(id2)
      } yield {
        Tuple3(response, firstRetrieved, secondRetrieved)
      }

      Await.result(endState, Duration.Inf) match {
        case Tuple3(response, first, second) =>
          first mustBe Some(data1)
          second mustBe None
          response mustBe true
      }
    }
  }

}


object RacDacRequestsQueueEventsLogRepositorySpec extends MockitoSugar {
  implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat

  private val mockConfiguration = mock[Configuration]

  private val idKey = "id"
  private val lastUpdatedKey = "lastUpdated"
  private val dataKey = "data"

  private val id1 = "id1"
  private val id2 = "id2"

  private val data1 = Json.obj("test" -> "test1")
  private val data2 = Json.obj("test" -> "test2")

  private val item1 = Json.obj(
    idKey -> id1,
    dataKey -> data1,
    lastUpdatedKey -> DateTime.now(DateTimeZone.UTC)
  )

  private val item2 = Json.obj(
    idKey -> id2,
    dataKey -> data2,
    lastUpdatedKey -> DateTime.now(DateTimeZone.UTC)
  )

  private val seqExistingData: Seq[JsObject] = Seq(
    item1, item2
  )

  private def buildFormRepository(mongoHost: String, mongoPort: Int): RacDacRequestsQueueEventsLogRepository = {
    val databaseName = "pensions-scheme-migration"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
    new RacDacRequestsQueueEventsLogRepository(MongoComponent(mongoUri), mockConfiguration)
  }
}

