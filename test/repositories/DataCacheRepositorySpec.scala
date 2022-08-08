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
import models.cache.MigrationLock
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.Configuration
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class DataCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with MongoEmbedDatabase with BeforeAndAfter with
  BeforeAndAfterEach { // scalastyle:off magic.number

  import DataCacheRepositorySpec._

  override def beforeEach: Unit = {
    super.beforeEach
    reset(mockLockCacheRepository, mockConfiguration)
    when(mockConfiguration.get[String](path = "mongodb.migration-cache.data-cache.name")).thenReturn("migration-data")
  }

  withEmbedMongoFixture(port = 24680) { _ =>

//    "getLockByPstr" must {
//      "get lock from Mongo collection" in {
//        mongoCollectionDrop()
//
//        val documentsInDB = for {
//          _ <- repository.collection.insertMany(
//            seqExistingData
//          ).toFuture
//
//          documentsInDB <- repository.getLockByPstr(pstr)
//        } yield documentsInDB
//
//        documentsInDB.map { documentsInDB =>
//          documentsInDB.size mustBe 1
//        }
//      }
//    }
//
//    "getLockByCredId" must {
//      "get lock from Mongo collection" in {
//        mongoCollectionDrop()
//
//        val documentsInDB = for {
//          _ <- repository.collection.insertMany(
//            seqExistingData
//          ).toFuture
//
//          documentsInDB <- repository.getLockByCredId(credId)
//        } yield documentsInDB
//
//        documentsInDB.map { documentsInDB =>
//          documentsInDB.size mustBe 1
//        }
//      }
//    }
//
//    "getLock" must {
//      "get lock from Mongo collection" in {
//        mongoCollectionDrop()
//
//        val documentsInDB = for {
//          _ <- repository.collection.insertMany(
//            seqExistingData
//          ).toFuture
//
//          documentsInDB <- repository.getLock(MigrationLock(pstr, credId, psaId))
//        } yield documentsInDB
//
//        documentsInDB.map { documentsInDB =>
//          documentsInDB.size mustBe 1
//        }
//      }
//    }

    "renewLockAndSave" must {
      "set lock in Mongo collection" in {
        mongoCollectionDrop()

        val data = JsString("test")
        when(mockLockCacheRepository.setLock(ArgumentMatchers.eq(migrationLock)))
          .thenReturn(Future.successful(true))

        val documentsInDB = for {
          status <- repository.renewLockAndSave(migrationLock, data)
          allDocs <- repository.collection.find().toFuture()
        } yield {
          Tuple2(allDocs.size, status)
        }

        documentsInDB.map { case Tuple2(totalDocs, status) =>
          status mustBe true
          totalDocs mustBe 1
          verify(mockLockCacheRepository, times(1)).setLock(migrationLock)

        }
      }
    }

//    "releaseLock" must {
//      "release lock from Mongo collection leaving other lock alone" in {
//        mongoCollectionDrop()
//
//        val endState = for {
//          _ <- repository.collection.insertMany(
//            seqExistingData
//          ).toFuture
//
//          response <- repository.releaseLock(MigrationLock(pstr, credId, psaId))
//          lock <- repository.getLock(MigrationLock(pstr, credId, psaId))
//          anotherLock <- repository.getLock(MigrationLock(anotherPstr, anotherCredId, anotherPsaId))
//        } yield {
//          Tuple3(response, lock, anotherLock)
//        }
//
//        endState.map { case Tuple3(response, migrationLock, anotherLock) =>
//          migrationLock mustBe None
//          anotherLock.isDefined mustBe true
//          response mustBe true
//        }
//      }
//    }
//
//    "releaseLockByPstr" must {
//      "release lock from Mongo collection leaving other lock alone" in {
//        mongoCollectionDrop()
//
//        val endState = for {
//          _ <- repository.collection.insertMany(
//            seqExistingData
//          ).toFuture
//
//          response <- repository.releaseLockByPstr(pstr)
//          lock <- repository.getLock(MigrationLock(pstr, credId, psaId))
//          anotherLock <- repository.getLock(MigrationLock(anotherPstr, anotherCredId, anotherPsaId))
//        } yield {
//          Tuple3(response, lock, anotherLock)
//        }
//
//        endState.map { case Tuple3(response, migrationLock, anotherLock) =>
//          migrationLock mustBe None
//          anotherLock.isDefined mustBe true
//          response mustBe true
//        }
//      }
//    }
//
//    "releaseLockByCredId" must {
//      "release lock from Mongo collection leaving other lock alone" in {
//        mongoCollectionDrop()
//
//        val endState = for {
//          _ <- repository.collection.insertMany(
//            seqExistingData
//          ).toFuture
//
//          response <- repository.releaseLockByCredId(pstr)
//          lock <- repository.getLock(MigrationLock(pstr, credId, psaId))
//          anotherLock <- repository.getLock(MigrationLock(anotherPstr, anotherCredId, anotherPsaId))
//        } yield {
//          Tuple3(response, lock, anotherLock)
//        }
//
//        endState.map { case Tuple3(response, migrationLock, anotherLock) =>
//          migrationLock mustBe None
//          anotherLock.isDefined mustBe true
//          response mustBe true
//        }
//      }
//    }

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

  private val repository = new DataCacheRepository(mockLockCacheRepository, mongoComponent, mockConfiguration)

  private val pstr = "pstr"
  private val credId = "credId"
  private val psaId = "psaId"

  private val anotherPstr = "pstr2"
  private val anotherCredId = "credId2"
  private val anotherPsaId = "psaId2"

  private val migrationLock = MigrationLock(pstr, credId, psaId)

//  val seqExistingData = Seq(
//    LockJson(
//      pstr = pstr,
//      credId = credId,
//      data = Json.toJson(MigrationLock(pstr, credId, psaId)),
//      lastUpdated = DateTime.now(DateTimeZone.UTC),
//      expireAt = DateTime.now(DateTimeZone.UTC).plusSeconds(60)
//    ),
//    LockJson(
//      pstr = anotherPstr,
//      credId = anotherCredId,
//      data = Json.toJson(MigrationLock(anotherPstr, anotherCredId, anotherPsaId)),
//      lastUpdated = DateTime.now(DateTimeZone.UTC),
//      expireAt = DateTime.now(DateTimeZone.UTC).plusSeconds(60)
//    )
//  )

}




