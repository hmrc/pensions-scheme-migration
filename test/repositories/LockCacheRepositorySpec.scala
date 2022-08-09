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
import models.cache.{LockJson, MigrationLock}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class LockCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with MongoEmbedDatabase with BeforeAndAfter with
  BeforeAndAfterEach { // scalastyle:off magic.number

  import LockCacheRepositorySpec._

  override def beforeEach: Unit = {
    super.beforeEach
    reset(mockConfiguration)
    when(mockConfiguration.get[String](ArgumentMatchers.eq( "mongodb.migration-cache.lock-cache.name"))(ArgumentMatchers.any()))
      .thenReturn("migration-lock")
    when(mockConfiguration.get[Int](ArgumentMatchers.eq("mongodb.migration-cache.lock-cache.timeToLiveInSeconds"))(ArgumentMatchers.any()))
      .thenReturn(900)
  }

  withEmbedMongoFixture(port = 24680) { _ =>

    "getLockByPstr" must {
      "get lock from Mongo collection" in {
        mongoCollectionDrop()

        val documentsInDB = for {
          _ <- repository.collection.insertMany(
            seqExistingData
          ).toFuture

          documentsInDB <- repository.getLockByPstr(pstr)
        } yield documentsInDB

        documentsInDB.map { documentsInDB =>
          documentsInDB.size mustBe 1
        }
      }
    }

    "getLockByCredId" must {
      "get lock from Mongo collection" in {
        mongoCollectionDrop()

        val documentsInDB = for {
          _ <- repository.collection.insertMany(
            seqExistingData
          ).toFuture

          documentsInDB <- repository.getLockByCredId(credId)
        } yield documentsInDB

        documentsInDB.map { documentsInDB =>
          documentsInDB.size mustBe 1
        }
      }
    }

    "getLock" must {
      "get lock from Mongo collection" in {
        mongoCollectionDrop()

        val documentsInDB = for {
          _ <- repository.collection.insertMany(
            seqExistingData
          ).toFuture

          documentsInDB <- repository.getLock(MigrationLock(pstr, credId, psaId))
        } yield documentsInDB

        documentsInDB.map { documentsInDB =>
          documentsInDB.size mustBe 1
        }
      }
    }

    "setLock" must {
      "set lock in Mongo collection" in {
        mongoCollectionDrop()

        val documentsInDB = for {
          documentsInDB <- repository.getLock(MigrationLock(pstr, credId, psaId))
        } yield documentsInDB

        documentsInDB.map { documentsInDB =>
          documentsInDB.size mustBe 1
        }
      }
    }

    "releaseLock" must {
      "release lock from Mongo collection leaving other lock alone" in {
        mongoCollectionDrop()

        val endState = for {
          _ <- repository.collection.insertMany(
            seqExistingData
          ).toFuture

          response <- repository.releaseLock(MigrationLock(pstr, credId, psaId))
          lock <- repository.getLock(MigrationLock(pstr, credId, psaId))
          anotherLock <- repository.getLock(MigrationLock(anotherPstr, anotherCredId, anotherPsaId))
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
        mongoCollectionDrop()

        val endState = for {
          _ <- repository.collection.insertMany(
            seqExistingData
          ).toFuture

          response <- repository.releaseLockByPstr(pstr)
          lock <- repository.getLock(MigrationLock(pstr, credId, psaId))
          anotherLock <- repository.getLock(MigrationLock(anotherPstr, anotherCredId, anotherPsaId))
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

    "releaseLockByCredId" must {
      "release lock from Mongo collection leaving other lock alone" in {
        mongoCollectionDrop()

        val endState = for {
          _ <- repository.collection.insertMany(
            seqExistingData
          ).toFuture

          response <- repository.releaseLockByCredId(pstr)
          lock <- repository.getLock(MigrationLock(pstr, credId, psaId))
          anotherLock <- repository.getLock(MigrationLock(anotherPstr, anotherCredId, anotherPsaId))
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
}


object LockCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockConfiguration = mock[Configuration]
  private val databaseName = "pensions-scheme-migration"
  private val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
  private val mongoComponent = MongoComponent(mongoUri)

  private def mongoCollectionDrop(): Void = Await
    .result(repository.collection.drop().toFuture(), Duration.Inf)

  private def repository = new LockCacheRepository(mongoComponent, mockConfiguration)

  private val pstr = "pstr"
  private val credId = "credId"
  private val psaId = "psaId"

  val anotherPstr = "pstr2"
  val anotherCredId = "credId2"
  val anotherPsaId = "psaId2"

  val seqExistingData = Seq(
    LockJson(
      pstr = pstr,
      credId = credId,
      data = Json.toJson(MigrationLock(pstr, credId, psaId)),
      lastUpdated = DateTime.now(DateTimeZone.UTC),
      expireAt = DateTime.now(DateTimeZone.UTC).plusSeconds(60)
    ),
    LockJson(
      pstr = anotherPstr,
      credId = anotherCredId,
      data = Json.toJson(MigrationLock(anotherPstr, anotherCredId, anotherPsaId)),
      lastUpdated = DateTime.now(DateTimeZone.UTC),
      expireAt = DateTime.now(DateTimeZone.UTC).plusSeconds(60)
    )
  )

}

