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

import crypto.DataEncryptor
import models.cache.{DataJson, MigrationLock}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.RecoverMethods.recoverToExceptionIf
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.mongo.MongoComponent
import org.mongodb.scala.gridfs.ObservableFuture

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}

class DataCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with BeforeAndAfter with BeforeAndAfterAll with BeforeAndAfterEach with ScalaFutures { // scalastyle:off magic.number

  import DataCacheRepositorySpec._
  import repositories.DataCacheRepository.LockCouldNotBeSetException

  var dataCacheRepository: DataCacheRepository = mock[DataCacheRepository]
  val mongoHost = "localhost"
  var mongoPort: Int = 27017

  private val modules: Seq[GuiceableModule] = Seq(
    bind[AuthConnector].toInstance(mock[AuthConnector]),
    bind[LockCacheRepository].toInstance(mock[LockCacheRepository]),
    bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
    bind[ListOfLegacySchemesCacheRepository].toInstance(mock[ListOfLegacySchemesCacheRepository]),
    bind[RacDacRequestsQueueRepository].toInstance(mock[RacDacRequestsQueueRepository]),
    bind[SchemeDataCacheRepository].toInstance(mock[SchemeDataCacheRepository]),
    bind[RacDacRequestsQueueEventsLogRepository].toInstance(mock[RacDacRequestsQueueEventsLogRepository])
  )

  private val app = new GuiceApplicationBuilder()
    .configure(
      conf = "auditing.enabled" -> false,
      "metrics.enabled" -> false,
      "metrics.jvm" -> false,
      "run.mode" -> "Test"
    ).overrides(modules*).build()

  private def buildFormRepository(mongoHost: String, mongoPort: Int): DataCacheRepository = {
    val databaseName = "pensions-scheme-migration"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
    new DataCacheRepository(mockLockCacheRepository, MongoComponent(mongoUri), mockConfiguration, app.injector.instanceOf[DataEncryptor])
  }

  override def beforeAll(): Unit = {
    when(mockConfiguration.get[String](ArgumentMatchers.eq("mongodb.migration-cache.data-cache.name"))(ArgumentMatchers.any()))
      .thenReturn("migration-data")
    when(mockConfiguration.get[Int](ArgumentMatchers.eq("mongodb.migration-cache.data-cache.timeToLiveInDays"))(ArgumentMatchers.any()))
      .thenReturn(28)
    dataCacheRepository = buildFormRepository(mongoHost, mongoPort)
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    Await.result(dataCacheRepository.collection.drop().toFuture(), 2.seconds)
    reset(mockLockCacheRepository)
    reset(mockConfiguration)
    super.beforeEach()
  }

  override def afterAll(): Unit = {
    Await.result(dataCacheRepository.collection.drop().toFuture(), 2.seconds)
    app.stop()
    super.afterAll()
  }


  "get" must {
    "get data from Mongo collection when present" in {

      when(mockLockCacheRepository.setLock(ArgumentMatchers.eq(migrationLock)))
        .thenReturn(Future.successful(true))

      when(mockLockCacheRepository.getLockByPstr(ArgumentMatchers.eq(pstr)))
        .thenReturn(Future.successful(None))

      val result = for {
        _ <- dataCacheRepository.collection.drop().toFuture()
        _ <- dataCacheRepository.renewLockAndSave(migrationLock, data)
        status <- dataCacheRepository.get(pstr)
        allDocs <- dataCacheRepository.collection.find().toFuture()
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

      when(mockLockCacheRepository.setLock(ArgumentMatchers.eq(migrationLock)))
        .thenReturn(Future.successful(true))

      when(mockLockCacheRepository.getLockByPstr(ArgumentMatchers.eq(pstr)))
        .thenReturn(Future.successful(None))

      val result = for {
        _ <- dataCacheRepository.collection.drop().toFuture()
        status <- dataCacheRepository.get(pstr)
        allDocs <- dataCacheRepository.collection.find().toFuture()
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

      when(mockLockCacheRepository.setLock(ArgumentMatchers.eq(migrationLock)))
        .thenReturn(Future.successful(true))

      when(mockLockCacheRepository.getLockByPstr(ArgumentMatchers.eq(pstr)))
        .thenReturn(Future.successful(None))

      val result = for {
        _ <- dataCacheRepository.collection.drop().toFuture()
        status <- dataCacheRepository.renewLockAndSave(migrationLock, data)
        allDocs <- dataCacheRepository.collection.find().toFuture()
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
      when(mockConfiguration.get[String](ArgumentMatchers.eq("mongodb.migration-cache.data-cache.name"))(ArgumentMatchers.any()))
        .thenReturn("migration-data")


      when(mockLockCacheRepository.setLock(ArgumentMatchers.eq(migrationLock)))
        .thenReturn(Future.successful(true))

      when(mockLockCacheRepository.getLockByPstr(ArgumentMatchers.eq(pstr)))
        .thenReturn(Future.successful(Some(migrationLock)))

      val result = for {
        _ <- dataCacheRepository.collection.drop().toFuture()
        status <- dataCacheRepository.renewLockAndSave(migrationLock, data)
        allDocs <- dataCacheRepository.collection.find().toFuture()
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

      when(mockLockCacheRepository.setLock(ArgumentMatchers.eq(migrationLock)))
        .thenReturn(Future.successful(true))

      when(mockLockCacheRepository.getLockByPstr(ArgumentMatchers.eq(pstr)))
        .thenReturn(Future.successful(Some(anotherMigrationLock)))

      val result = for {
        _ <- dataCacheRepository.collection.drop().toFuture()
        status <- dataCacheRepository.renewLockAndSave(migrationLock, data)
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

      when(mockLockCacheRepository.releaseLockByPstr(ArgumentMatchers.eq(pstr)))
        .thenReturn(Future.successful(true))

      val endState = for {
        _ <- dataCacheRepository.collection.drop().toFuture()
        _ <- dataCacheRepository.collection.insertMany(seqExistingData).toFuture()

        response <- dataCacheRepository.remove(pstr)
        firstRetrieved <- dataCacheRepository.get(pstr)
        secondRetrieved <- dataCacheRepository.get(anotherPstr)
      } yield {
        Tuple3(response, firstRetrieved, secondRetrieved)
      }

      Await.result(endState, Duration.Inf) match {
        case Tuple3(response, _, secondRetrieved) =>
          //firstRetrieved mustBe None
          secondRetrieved.isDefined mustBe true
          response mustBe true
      }
    }
  }


}

object DataCacheRepositorySpec extends MockitoSugar {


  private val mockConfiguration = mock[Configuration]

  private val mockLockCacheRepository = mock[LockCacheRepository]

  private val pstr = "pstr"
  private val credId = "credId"
  private val psaId = "psaId"

  private val anotherPstr = "pstr2"
  private val anotherPsaId = "psaId2"

  private val migrationLock = MigrationLock(pstr, credId, psaId)
  private val anotherMigrationLock = MigrationLock(pstr, credId, anotherPsaId)

  private val data = Json.obj("test" -> "test")

  private val seqExistingData = Seq(
    DataJson(
      pstr = pstr,
      data = data,
      lastUpdated = Instant.now(),
      expireAt = Instant.now().plusSeconds(60)
    ),
    DataJson(
      pstr = anotherPstr,
      data = data,
      lastUpdated = Instant.now(),
      expireAt = Instant.now().plusSeconds(60)
    )
  )
}

