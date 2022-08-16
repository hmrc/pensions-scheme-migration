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

import com.github.simplyscala.MongoEmbedDatabase
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.Configuration
import play.api.libs.json.{Format, JsObject, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class RacDacRequestsQueueEventsLogRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with MongoEmbedDatabase with BeforeAndAfter with
  BeforeAndAfterEach { // scalastyle:off magic.number

  import RacDacRequestsQueueEventsLogRepositorySpec._

  override def beforeEach: Unit = {
    super.beforeEach
    reset(mockLockCacheRepository, mockConfiguration)
    when(mockConfiguration.get[String](ArgumentMatchers.eq( "mongodb.migration-cache.rac-dac-requests-queue-events-log.name"))(ArgumentMatchers.any()))
      .thenReturn("rac-dac-requests-queue-events-log")
    when(mockConfiguration.get[Int](ArgumentMatchers.eq("mongodb.migration-cache.rac-dac-requests-queue-events-log.timeToLiveInSeconds"))(ArgumentMatchers.any()))
      .thenReturn(3600)
  }

  withEmbedMongoFixture(port = 24680) { _ =>

    "get" must {
      "get data from Mongo collection when present" in {
        mongoCollectionDrop()
        val result = for {
          _ <- repository.collection.insertMany(seqExistingData).toFuture
          status <- repository.get(id1)
        } yield {
          status
        }

        Await.result(result, Duration.Inf) match {
          case status =>
            status mustBe Some(data1)
        }
      }

      "get None from Mongo collection when not present" in {
        mongoCollectionDrop()
        val result = for {
          _ <- repository.collection.insertMany(seqExistingData).toFuture
          status <- repository.get("dummyId")
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
        mongoCollectionDrop()

        val result = for {
          status <- repository.save(id2, data2)
          allDocs <- repository.collection.find().toFuture()
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
        mongoCollectionDrop()

        val result = for {
          _ <- repository.collection.insertMany(seqExistingData).toFuture
          _ <- repository.save(id2, data1)
          updatedItem <- repository.get(id2)
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
        mongoCollectionDrop()

        val endState = for {
          _ <- repository.collection.insertMany(seqExistingData).toFuture
          response <- repository.remove(id2)
          firstRetrieved <- repository.get(id1)
          secondRetrieved <- repository.get(id2)
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
}


object RacDacRequestsQueueEventsLogRepositorySpec extends AnyWordSpec with MockitoSugar {
  implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockConfiguration = mock[Configuration]
  private val databaseName = "pensions-scheme-migration"
  private val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
  private val mongoComponent = MongoComponent(mongoUri)

  private def mongoCollectionDrop(): Void = Await
    .result(repository.collection.drop().toFuture(), Duration.Inf)

  private val mockLockCacheRepository = mock[LockCacheRepository]

  private def repository = new RacDacRequestsQueueEventsLogRepository(mongoComponent, mockConfiguration)

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
}

