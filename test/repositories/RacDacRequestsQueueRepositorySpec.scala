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

import audit.AuditServiceSpec.mock
import com.typesafe.config.ConfigFactory
import config.AppConfig
import crypto.DataEncryptor
import models.racDac.{RacDacHeaders, RacDacRequest, WorkItemRequest}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Configuration
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.mongo.workitem.ProcessingStatus._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

class RacDacRequestsQueueRepositorySpec extends AnyWordSpec with Matchers with MongoSupport with BeforeAndAfterAll with BeforeAndAfterEach {

  private val config = Configuration(
    ConfigFactory.parseString(
      """
        |racDacWorkItem {
        |    submission-poller {
        |        initial-delay = 10 seconds
        |        interval = 1 seconds
        |        failure-count-limit = 10
        |        in-progress-retry-after = 1000
        |        mongo {
        |            ttl = 604800 seconds # 7 days
        |        }
        |    }
        |}
        |mongodb.migration-cache.racDac-work-item-queue.name = "racDac-work-item"
        |""".stripMargin
    )
  )


  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.mongoEncryptionKey).thenReturn(None)
  private val dataEncryptor = new DataEncryptor(mockAppConfig)
  private val repository = new RacDacRequestsQueueRepository(config,
    MongoComponent(mongoUri),
    new ServicesConfig(config),
    dataEncryptor
  )
  private val racDacRequest = WorkItemRequest("test psa",
    RacDacRequest("test scheme 1", "001","00615269RH","2012-02-20","2020-01-01"), RacDacHeaders(None, None))
  private val racDacRequestEncrypted = racDacRequest.encrypt(dataEncryptor)

  override def beforeEach(): Unit = {
    prepareDatabase()
    super.beforeEach()
  }

  "RacDacRequestsQueueRepository" when {

    "pushAll" must {
      "insert all rac dac requests" in {
        await(repository.pushAll(Seq(racDacRequest))).map(item => item.map(_.item)) mustBe Right(Seq(racDacRequestEncrypted))
      }
    }

    "push" must {
      "insert rac dac request" in {
        await(repository.push(racDacRequest)).map(_.item) mustBe Right(racDacRequestEncrypted)
      }
    }

    "pull" must {
      "return some work item if one exists" in {
        await(repository.pushAll(Seq(racDacRequest))).map(item => item.map(_.item)) mustBe Right(Seq(racDacRequestEncrypted))

        await(repository.pull).map(maybeWorkItem => maybeWorkItem.map(workItem => workItem.item)) mustBe Right(
          Some(racDacRequest)
        )
      }

      "return none if no work item exists " in {
        await(
          repository.pull
        ).map(mw => mw.map(s => s.item)) mustBe Right(None)
      }
    }

    "set processing status" should {

      "update the work item status" in {
        val workItem = await(repository.pushAll(Seq(racDacRequest)))
        workItem.map(wi => wi.map(req => await(repository.setProcessingStatus(req.id, Failed)) mustBe Right(true)))
      }
    }

    "set result status" should {
      "update the work item status" in {
        val workItem = await(repository.pushAll(Seq(racDacRequest)))
        val _ = workItem.map(wi => wi.map(req => await(repository.setProcessingStatus(req.id, InProgress))))
        workItem.map(wi => wi.map(req => await(repository.setResultStatus(req.id, PermanentlyFailed)) mustBe Right(true)))
      }
    }

    "get total no of requests" should {
      "return no of requests in the queue" in {
        val _ = await(repository.pushAll(Seq(racDacRequest)))
        val noOfRequests = await(repository.getTotalNoOfRequestsByPsaId("test psa"))
        noOfRequests mustEqual Right(1)
      }
    }

    "get no of failures" should {
      "return no of failed messages in the queue" in {
        val workItem = await(repository.pushAll(Seq(racDacRequest)))
        val _ = workItem.map(wi => wi.map(req => await(repository.setProcessingStatus(req.id, PermanentlyFailed))))
        val noOfRequests = await(repository.getNoOfFailureByPsaId("test psa"))
        noOfRequests mustEqual Right(1)
      }
    }

    "delete all request" should {
      "return true if all the requests are deleted" in {
        val res = await(repository.deleteAll("test psa"))
        res mustEqual Right(true)
      }
    }
  }
}
