/*
 * Copyright 2021 HM Revenue & Customs
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

import com.typesafe.config.ConfigFactory
import models.racDac.RacDacHeaders
import org.scalatest.{MustMatchers, WordSpec}
import play.api.Configuration
import play.api.test.Helpers._
import service.Request
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.workitem.{Failed, InProgress, PermanentlyFailed}

import scala.concurrent.ExecutionContext.Implicits.global

class RacDacRequestsQueueRepositorySpec extends WordSpec with MustMatchers with MongoSupport {
  val config = Configuration(
    ConfigFactory.parseString(
      """
        |racDacWorkItem {
        |    queue-name = "queue-name"
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

  private val repository = new RacDacRequestsQueueRepository(config, reactiveMongoComponent, new ServicesConfig(config))
  private val racDacRequest = RacDacRequest("", Request("", ""), RacDacHeaders(None, None))

  "RacDacRequestsQueueRepository" when {

    "push" must {
      "insert a rac dac request" in {
        await(repository.push(racDacRequest)).map(item => item.item) mustBe Right(racDacRequest)
      }
    }

    "pushAll" must {
      "insert all rac dac requests" in {
        await(repository.pushAll(Seq(racDacRequest))).map(item => item.map(_.item)) mustBe Right(Seq(racDacRequest))
      }
    }

    "pull" must {
      "return some work item if one exists" in {
        await(repository.push(racDacRequest)).map(item => item.item) mustBe Right(racDacRequest)

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
        val workItem = await(repository.push(racDacRequest))
        workItem.map(wi => await(repository.setProcessingStatus(wi.id, Failed)) mustBe Right(true))
      }
    }

    "set result status" should {
      "update the work item status" in {
        val workItem = await(repository.push(racDacRequest))
        val _ = workItem.map(wi => await(repository.setProcessingStatus(wi.id, InProgress)))
        workItem.map(wi => await(repository.setResultStatus(wi.id, PermanentlyFailed)) mustBe Right(true))
      }
    }
  }
}
