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

package service

import akka.actor.ActorSystem
import akka.util.Timeout
import connector.SchemeConnector
import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers.await
import reactivemongo.bson.BSONObjectID
import repositories.RacDacRequestsQueueRepository
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.workitem.{Failed, PermanentlyFailed, ToDo, WorkItem}

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class RacDacBulkSubmissionServiceSpec() extends WordSpec with MustMatchers with MockitoSugar {

  implicit val timeout: Timeout = Timeout(FiniteDuration(5, TimeUnit.SECONDS))

  private val mockRacDacSubmissionRepo = mock[RacDacRequestsQueueRepository]
  private val mockSchemeConnector = mock[SchemeConnector]

  private val actorSystem                                  = ActorSystem()
  implicit val dmsSubmissionPollerExecutionContext = new RacDacBulkSubmissionPollerExecutionContext(actorSystem)

  private val racDacBulkSubmissionService = new RacDacBulkSubmissionService(mockRacDacSubmissionRepo, mockSchemeConnector)

  private val racDacRequest = RacDacRequest("test psa id", Request("test rac dac", "123456"), RacDacHeaders(None, None))

  "RacDac Bulk Submission Service" when {

    "the submission poller requests a work item" must {
      "dequeue the next work item" in {
        val workItem: WorkItem[RacDacRequest] = WorkItem(BSONObjectID.generate(), DateTime.now(), DateTime.now(), DateTime.now(), ToDo, 0,
          racDacRequest)
        when(mockRacDacSubmissionRepo.pull).thenReturn(Future(Right(Some(workItem))))
        await(racDacBulkSubmissionService.dequeue) mustBe Right(Some(workItem))
      }
    }

    "a dms submission request is made" must {
      "enqueue the request" in {
        val workItem: WorkItem[RacDacRequest] = WorkItem(BSONObjectID.generate(), DateTime.now(),
          DateTime.now(), DateTime.now(), ToDo, 0,racDacRequest)

        when(mockRacDacSubmissionRepo.pushAll(any())).thenReturn(Future(Right(Seq(workItem))))
        await(racDacBulkSubmissionService.enqueue(Seq(racDacRequest))) mustBe true
      }
    }

    "the submission poller updates the processing status" must {
      "return true to indicate that the status has been updated" in {
        when(mockRacDacSubmissionRepo.setProcessingStatus(any(), any())).thenReturn(Future(Right(true)))
        await(racDacBulkSubmissionService.setProcessingStatus(BSONObjectID.generate(), Failed)) mustBe Right(true)
      }
    }

    "the submission poller updates the complete status" must {
      "return true to indicate that the status has been updated" in {
        val workItem: WorkItem[RacDacRequest] = WorkItem(BSONObjectID.generate(), DateTime.now(),
          DateTime.now(), DateTime.now(), ToDo, 0,racDacRequest)
        when(mockRacDacSubmissionRepo.setResultStatus(any(), any())).thenReturn(Future(Right(true)))
        await(racDacBulkSubmissionService.setResultStatus(workItem.id, PermanentlyFailed)) mustBe Right(true)
      }
    }

    "the submission poller pulls the work item" must {
      "submit the request to ETMP successfully" in {
        when(mockSchemeConnector.registerRacDac(any(), any())(any(), any())).thenReturn(Future(Right(Json.obj())))
        await(racDacBulkSubmissionService.submitToETMP(racDacRequest)) mustBe Right(Json.obj())
      }

      "failed submission to ETMP" in {
        val iException = new InternalServerException("Error")
        when(mockSchemeConnector.registerRacDac(any(), any())(any(), any())).thenReturn(Future(Left(iException)))
        await(racDacBulkSubmissionService.submitToETMP(racDacRequest)) mustBe Left(iException)
      }
    }
  }

}
