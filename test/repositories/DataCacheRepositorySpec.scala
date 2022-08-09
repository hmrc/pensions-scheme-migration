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

import com.github.nscala_time.time.Imports.DateTimeZone
import com.github.simplyscala.MongoEmbedDatabase
import models.cache.{DataJson, MigrationLock}
import org.joda.time.DateTime
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.RecoverMethods.recoverToExceptionIf
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class DataCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with MongoEmbedDatabase with BeforeAndAfter with
  BeforeAndAfterEach { // scalastyle:off magic.number

  import DataCacheRepositorySpec._
  import repositories.DataCacheRepository.LockCouldNotBeSetException

  override def beforeEach: Unit = {
    super.beforeEach
    reset(mockLockCacheRepository, mockConfiguration)
    when(mockConfiguration.get[String](ArgumentMatchers.eq( "mongodb.migration-cache.data-cache.name"))(ArgumentMatchers.any()))
      .thenReturn("migration-data")
    when(mockConfiguration.get[Int](ArgumentMatchers.eq( "mongodb.migration-cache.data-cache.timeToLiveInDays"))(ArgumentMatchers.any()))
      .thenReturn(28)
  }

  withEmbedMongoFixture(port = 24680) { _ =>

    "get" must {
      "get data from Mongo collection when present" in {
        mongoCollectionDrop()

        when(mockLockCacheRepository.setLock(ArgumentMatchers.eq(migrationLock)))
          .thenReturn(Future.successful(true))

        when(mockLockCacheRepository.getLockByPstr(ArgumentMatchers.eq(pstr)))
          .thenReturn(Future.successful(None))

        val result = for {
          _ <- repository.renewLockAndSave(migrationLock, data)
          status <- repository.get(pstr)
          allDocs <- repository.collection.find().toFuture()
        } yield {
          Tuple2(allDocs.size, status)
        }

        Await.result(result, Duration.Inf) match {
          case Tuple2(totalDocs, optionJson) =>
            optionJson.isDefined mustBe true
            optionJson.map { jsValue =>
              (jsValue \ "test").asOpt[String] mustBe Some("test")
            }
            totalDocs mustBe 1
        }
      }

      "return None from Mongo collection when not present" in {
        mongoCollectionDrop()

        when(mockLockCacheRepository.setLock(ArgumentMatchers.eq(migrationLock)))
          .thenReturn(Future.successful(true))

        when(mockLockCacheRepository.getLockByPstr(ArgumentMatchers.eq(pstr)))
          .thenReturn(Future.successful(None))

        val result = for {
          status <- repository.get(pstr)
          allDocs <- repository.collection.find().toFuture()
        } yield {
          Tuple2(allDocs.size, status)
        }

        Await.result(result, Duration.Inf) match {
          case Tuple2(totalDocs, optionJson) =>
            optionJson.isDefined mustBe false
            totalDocs mustBe 0
        }
      }
    }


    "renewLockAndSave" must {
      "set lock in Mongo collection where there are no current locks" in {
        mongoCollectionDrop()

        when(mockLockCacheRepository.setLock(ArgumentMatchers.eq(migrationLock)))
          .thenReturn(Future.successful(true))

        when(mockLockCacheRepository.getLockByPstr(ArgumentMatchers.eq(pstr)))
          .thenReturn(Future.successful(None))

        val result = for {
          status <- repository.renewLockAndSave(migrationLock, data)
          allDocs <- repository.collection.find().toFuture()
        } yield {
          Tuple2(allDocs.size, status)
        }

        Await.result(result, Duration.Inf) match {
          case Tuple2(totalDocs, status) =>
            status mustBe true
            totalDocs mustBe 1
            verify(mockLockCacheRepository, times(1)).setLock(migrationLock)
        }
      }


      "set lock in Mongo collection where there is current lock but it is locked for current pstr/ psa" in {
        when(mockConfiguration.get[String](ArgumentMatchers.eq( "mongodb.migration-cache.data-cache.name"))(ArgumentMatchers.any()))
          .thenReturn("migration-data")
        mongoCollectionDrop()

        when(mockLockCacheRepository.setLock(ArgumentMatchers.eq(migrationLock)))
          .thenReturn(Future.successful(true))

        when(mockLockCacheRepository.getLockByPstr(ArgumentMatchers.eq(pstr)))
          .thenReturn(Future.successful(Some(migrationLock)))

        val result = for {
          status <- repository.renewLockAndSave(migrationLock, data)
          allDocs <- repository.collection.find().toFuture()
        } yield {
          Tuple2(allDocs.size, status)
        }

        Await.result(result, Duration.Inf) match {
          case Tuple2(totalDocs, status) =>
            status mustBe true
            totalDocs mustBe 1
            verify(mockLockCacheRepository, times(1)).setLock(migrationLock)
        }
      }

      "throw LockCouldNotBeSetException where there is current lock but it is locked for different pstr/ psa" in {
        when(mockConfiguration.get[String](ArgumentMatchers.eq("mongodb.migration-cache.data-cache.name"))(ArgumentMatchers.any()))
          .thenReturn("migration-data")
        mongoCollectionDrop()

        when(mockLockCacheRepository.setLock(ArgumentMatchers.eq(migrationLock)))
          .thenReturn(Future.successful(true))

        when(mockLockCacheRepository.getLockByPstr(ArgumentMatchers.eq(pstr)))
          .thenReturn(Future.successful(Some(anotherMigrationLock)))

        val result = for {
          status <- repository.renewLockAndSave(migrationLock, data)
        } yield {
          status
        }

        recoverToExceptionIf[Exception] {
          result
        } map {
          _.getMessage mustBe LockCouldNotBeSetException.getMessage
        }
      }
    }

    "remove" must {
      "remove lock from Mongo collection leaving other one" in {
        mongoCollectionDrop()

        when(mockLockCacheRepository.releaseLockByPstr(ArgumentMatchers.eq(pstr)))
          .thenReturn(Future.successful(true))

        val endState = for {
          _ <- repository.collection.insertMany(
            seqExistingData
          ).toFuture

          response <- repository.remove(pstr)
          firstRetrieved <- repository.get(pstr)
          secondRetrieved <- repository.get(anotherPstr)
        } yield {
          Tuple3(response, firstRetrieved, secondRetrieved)
        }

        Await.result(endState, Duration.Inf) match { case Tuple3(response, migrationLock, anotherLock) =>
          migrationLock mustBe None
          anotherLock.isDefined mustBe true
          response mustBe true
        }
      }
    }

  }
}

object DataCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockConfiguration = mock[Configuration]
  private val databaseName = "pensions-scheme-migration"
  private val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
  private val mongoComponent = MongoComponent(mongoUri)

  private def mongoCollectionDrop(): Void = Await
    .result(repository.collection.drop().toFuture(), Duration.Inf)

  private val mockLockCacheRepository = mock[LockCacheRepository]

  private def repository = new DataCacheRepository(mockLockCacheRepository, mongoComponent, mockConfiguration)

  private val pstr = "pstr"
  private val credId = "credId"
  private val psaId = "psaId"

  private val anotherPstr = "pstr2"
  private val anotherCredId = "credId2"
  private val anotherPsaId = "psaId2"

  private val migrationLock = MigrationLock(pstr, credId, psaId)
  private val anotherMigrationLock = MigrationLock(pstr, credId, anotherPsaId)

  private val data = Json.obj("test" -> "test")

  private val seqExistingData = Seq(
    DataJson(
      pstr = pstr,
      data = data,
      lastUpdated = DateTime.now(DateTimeZone.UTC),
      expireAt = DateTime.now(DateTimeZone.UTC).plusSeconds(60)
    ),
    DataJson(
      pstr = anotherPstr,
      data = data,
      lastUpdated = DateTime.now(DateTimeZone.UTC),
      expireAt = DateTime.now(DateTimeZone.UTC).plusSeconds(60)
    )
  )

}




