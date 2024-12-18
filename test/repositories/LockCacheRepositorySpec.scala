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

import models.cache.{LockJson, MigrationLock}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.MongoComponent

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global


class LockCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with BeforeAndAfter with
  BeforeAndAfterAll with BeforeAndAfterEach with ScalaFutures { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  import LockCacheRepositorySpec._

  var lockCacheRepository: LockCacheRepository = _
  val mongoHost = "localhost"
  var mongoPort: Int = 27017
  override def beforeAll(): Unit = {

    when(mockConfiguration.get[String](ArgumentMatchers.eq("mongodb.migration-cache.lock-cache.name"))(ArgumentMatchers.any()))
      .thenReturn("migration-lock")
    when(mockConfiguration.get[Int](ArgumentMatchers.eq("mongodb.migration-cache.lock-cache.timeToLiveInSeconds"))(ArgumentMatchers.any()))
      .thenReturn(900)

    lockCacheRepository = buildFormRepository(mongoHost, mongoPort)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    Await.result(lockCacheRepository.collection.drop().toFuture(), Duration.Inf)
  }

  override def beforeEach(): Unit = {
    reset(mockConfiguration)
    Await.result(lockCacheRepository.collection.drop().toFuture(), Duration.Inf)
    super.beforeEach()
  }

  "getLockByPstr" must {
    "get lock from Mongo collection" in {

      val documentsInDB = for {
        _ <- lockCacheRepository.collection.drop().toFuture()
        _ <- lockCacheRepository.collection.insertMany(
          seqExistingData
        ).toFuture()

        documentsInDB <- lockCacheRepository.getLockByPstr(pstr)
      } yield documentsInDB

      documentsInDB.map { documentsInDB =>
        documentsInDB.size mustBe 1
      }
    }
  }

  "getLockByCredId" must {
    "get lock from Mongo collection" in {

      val documentsInDB = for {
        _ <- lockCacheRepository.collection.drop().toFuture()
        _ <- lockCacheRepository.collection.insertMany(
          seqExistingData
        ).toFuture()

        documentsInDB <- lockCacheRepository.getLockByCredId(credId)
      } yield documentsInDB

      documentsInDB.map { documentsInDB =>
        documentsInDB.size mustBe 1
      }
    }
  }

  "getLock" must {
    "get lock from Mongo collection" in {

      val documentsInDB = for {
        _ <- lockCacheRepository.collection.drop().toFuture()
        _ <- lockCacheRepository.collection.insertMany(
          seqExistingData
        ).toFuture()

        documentsInDB <- lockCacheRepository.getLock(MigrationLock(pstr, credId, psaId))
      } yield documentsInDB

      documentsInDB.map { documentsInDB =>
        documentsInDB.size mustBe 1
      }
    }
  }

  "setLock" must {
    "set lock in Mongo collection" in {

      val documentsInDB = for {
        _ <- lockCacheRepository.collection.drop().toFuture()
        documentsInDB <- lockCacheRepository.getLock(MigrationLock(pstr, credId, psaId))
      } yield documentsInDB

      documentsInDB.map { documentsInDB =>
        documentsInDB.size mustBe 1
      }
    }
  }

  "releaseLock" must {
    "release lock from Mongo collection leaving other lock alone" in {

      val endState = for {
        _ <- lockCacheRepository.collection.drop().toFuture()
        _ <- lockCacheRepository.collection.insertMany(
          seqExistingData
        ).toFuture()

        response <- lockCacheRepository.releaseLock(MigrationLock(pstr, credId, psaId))
        lock <- lockCacheRepository.getLock(MigrationLock(pstr, credId, psaId))
        anotherLock <- lockCacheRepository.getLock(MigrationLock(anotherPstr, anotherCredId, anotherPsaId))
      } yield {
        Tuple3(response, lock, anotherLock)
      }

      endState.map { case Tuple3(response, migrationLock, anotherLock) =>
        migrationLock mustBe None
        anotherLock.isDefined mustBe true
        response mustBe true
      }
    }
  }

  "releaseLockByPstr" must {
    "release lock from Mongo collection leaving other lock alone" in {

      val endState = for {
        _ <- lockCacheRepository.collection.drop().toFuture()
        _ <- lockCacheRepository.collection.insertMany(
          seqExistingData
        ).toFuture()

        response <- lockCacheRepository.releaseLockByPstr(pstr)
        lock <- lockCacheRepository.getLock(MigrationLock(pstr, credId, psaId))
        anotherLock <- lockCacheRepository.getLock(MigrationLock(anotherPstr, anotherCredId, anotherPsaId))
      } yield {
        Tuple3(response, lock, anotherLock)
      }

      endState.map { case Tuple3(response, migrationLock, anotherLock) =>
        migrationLock mustBe None
        anotherLock.isDefined mustBe true
        response mustBe true
      }
    }
  }

}

object LockCacheRepositorySpec extends MockitoSugar {

  private val mockConfiguration = mock[Configuration]

  private val pstr = "pstr"
  private val credId = "credId"
  private val psaId = "psaId"

  val anotherPstr = "pstr2"
  val anotherCredId = "credId2"
  val anotherPsaId = "psaId2"

  val seqExistingData: Seq[LockJson] = Seq(
    LockJson(
      pstr = pstr,
      credId = credId,
      data = Json.toJson(MigrationLock(pstr, credId, psaId)),
      lastUpdated = Instant.now(),
      expireAt = Instant.now().plusSeconds(60)
    ),
    LockJson(
      pstr = anotherPstr,
      credId = anotherCredId,
      data = Json.toJson(MigrationLock(anotherPstr, anotherCredId, anotherPsaId)),
      lastUpdated = Instant.now(),
      expireAt = Instant.now().plusSeconds(60)
    )
  )
  val testRepositoryName = "testRepository" +  UUID.randomUUID().toString
  private def buildFormRepository(mongoHost: String, mongoPort: Int): LockCacheRepository = {
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$testRepositoryName?heartbeatFrequencyMS=1000"
    new LockCacheRepository(MongoComponent(mongoUri), mockConfiguration)
  }
}

