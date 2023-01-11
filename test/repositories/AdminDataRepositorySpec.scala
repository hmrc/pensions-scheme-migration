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

import models.FeatureToggle
import models.FeatureToggleName.DummyToggle
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import repositories.FeatureToggleMongoFormatter.{FeatureToggles, featureToggles, id}
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext.Implicits.global


class AdminDataRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with EmbeddedMongoDBSupport with BeforeAndAfter with
  BeforeAndAfterAll with ScalaFutures { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  import AdminDataRepositorySpec._

  var adminDataRepository: AdminDataRepository = _

  override def beforeAll(): Unit = {
    when(mockAppConfig.get[String](path = "mongodb.migration-cache.admin-data.name")).thenReturn("admin-data")
    initMongoDExecutable()
    startMongoD()
    adminDataRepository = buildFormRepository(mongoHost, mongoPort)
    super.beforeAll()
  }

  override def afterAll(): Unit =
    stopMongoD()

  "getFeatureToggle" must {
    "get FeatureToggles from Mongo collection" in {


      val documentsInDB = for {
        _ <- adminDataRepository.collection.drop().toFuture()
        _ <- adminDataRepository.collection.insertOne(
          FeatureToggles("toggles", Seq(FeatureToggle(DummyToggle, enabled = true)))).headOption()
        documentsInDB <- adminDataRepository.getFeatureToggles
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.size mustBe 1
      }
    }
  }

  "setFeatureToggle" must {
    "set new FeatureToggles in Mongo collection" in {

      val documentsInDB = for {
        _ <- adminDataRepository.collection.drop().toFuture()
        _ <- adminDataRepository.setFeatureToggles(Seq(FeatureToggle(DummyToggle, enabled = true)))
        documentsInDB <- adminDataRepository.collection.find[FeatureToggles](Filters.eq(id, featureToggles)).headOption()
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.map(_.toggles.size mustBe 1)
      }
    }

    "set empty FeatureToggles in Mongo collection" in {

      val documentsInDB = for {
        _ <- adminDataRepository.collection.drop().toFuture()
        _ <- adminDataRepository.setFeatureToggles(Seq.empty)
        documentsInDB <- adminDataRepository.collection.find[FeatureToggles]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.map(_.toggles.size mustBe 0)
      }
    }
  }
}

object AdminDataRepositorySpec extends MockitoSugar {

  private val mockAppConfig = mock[Configuration]

  private def buildFormRepository(mongoHost: String, mongoPort: Int): AdminDataRepository = {
    val databaseName = "pensions-scheme-migration"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
    new AdminDataRepository(MongoComponent(mongoUri), mockAppConfig)
  }
}
