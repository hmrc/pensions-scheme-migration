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

package service

import akka.actor.ActorSystem
import akka.util.Timeout
import models.racDac.{RacDacHeaders, RacDacRequest, WorkItemRequest}
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers.await
import repositories.RacDacRequestsQueueRepository
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Failed, PermanentlyFailed, ToDo}
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class RacDacBulkSubmissionServiceSpec() extends AnyWordSpec with Matchers with MockitoSugar with EitherValues { // scalastyle:off magic.number

  implicit val timeout: Timeout = Timeout(FiniteDuration(5, TimeUnit.SECONDS))

  private val mockRacDacSubmissionRepo = mock[RacDacRequestsQueueRepository]
  private val mockPensionSchemeService = mock[PensionSchemeService]

  private val actorSystem = ActorSystem()
  implicit val dmsSubmissionPollerExecutionContext: RacDacBulkSubmissionPollerExecutionContext = new RacDacBulkSubmissionPollerExecutionContext(actorSystem)

  private val racDacBulkSubmissionService = new RacDacBulkSubmissionService(mockRacDacSubmissionRepo, mockPensionSchemeService)

  private val racDacRequest = WorkItemRequest("test psa id",
    RacDacRequest("test rac dac", "123456", "00615269RH", "2012-02-20", "2020-01-01"), RacDacHeaders(None, None))

  "RacDac Bulk Submission Service" when {

    "the submission poller requests a work item" must {
      "dequeue the next work item" in {
        val workItem: WorkItem[WorkItemRequest] = WorkItem(ObjectId.get(), Instant.now(), Instant.now(), Instant.now(), ToDo, 0,
          racDacRequest)
        when(mockRacDacSubmissionRepo.pull).thenReturn(Future(Right(Some(workItem))))
        await(racDacBulkSubmissionService.dequeue) mustBe Right(Some(workItem))
      }

      "return exception if there is an exception pulling item from the queue" in {
        val ex = new Exception("message")
        when(mockRacDacSubmissionRepo.pull).thenReturn(Future(Left(ex)))
        await(racDacBulkSubmissionService.dequeue) mustBe Left(ex)
      }
    }

    "a dms submission request is made" must {
      "return true after successfully enqueue the request" in {
        reset(mockRacDacSubmissionRepo)
        val workItem: WorkItem[WorkItemRequest] = WorkItem(ObjectId.get(), Instant.now(),
          Instant.now(), Instant.now(), ToDo, 0, racDacRequest)

        when(mockRacDacSubmissionRepo.pushAll(any)).thenReturn(Future(Right(Seq(workItem))))
        when(mockRacDacSubmissionRepo.push(any)).thenReturn(Future(Right(workItem)))
        await(racDacBulkSubmissionService.enqueue(Seq(racDacRequest))) mustBe true
        verify(mockRacDacSubmissionRepo, times(1)).pushAll(any)
      }

      "return false if there is an error enqueuing the request" in {
        reset(mockRacDacSubmissionRepo)
        val ex = new Exception("message")
        when(mockRacDacSubmissionRepo.push(any)).thenReturn(Future(Left(ex)))
        await(racDacBulkSubmissionService.enqueue(Seq(racDacRequest))) mustBe false
        verify(mockRacDacSubmissionRepo, never).pushAll(any)
      }
    }

    "the submission poller updates the processing status" must {
      "return true to indicate that the status has been updated" in {
        when(mockRacDacSubmissionRepo.setProcessingStatus(any, any)).thenReturn(Future(Right(true)))
        await(racDacBulkSubmissionService.setProcessingStatus(ObjectId.get(), Failed)) mustBe Right(true)
      }

      "return error if error occurred" in {
        val ex = new Exception("message")
        when(mockRacDacSubmissionRepo.setProcessingStatus(any, any)).thenReturn(Future(Left(ex)))
        await(racDacBulkSubmissionService.setProcessingStatus(ObjectId.get(), Failed)) mustBe Left(ex)
      }
    }

    "the submission poller updates the complete status" must {
      "return true to indicate that the status has been updated" in {
        val workItem: WorkItem[WorkItemRequest] = WorkItem(ObjectId.get(), Instant.now(),
          Instant.now(), Instant.now(), ToDo, 0, racDacRequest)
        when(mockRacDacSubmissionRepo.setResultStatus(any, any)).thenReturn(Future(Right(true)))
        await(racDacBulkSubmissionService.setResultStatus(workItem.id, PermanentlyFailed)) mustBe Right(true)
      }

      "return error if error occurred" in {
        val workItem: WorkItem[WorkItemRequest] = WorkItem(ObjectId.get(), Instant.now(),
          Instant.now(), Instant.now(), ToDo, 0, racDacRequest)
        val ex = new Exception("message")
        when(mockRacDacSubmissionRepo.setResultStatus(any, any)).thenReturn(Future(Left(ex)))
        await(racDacBulkSubmissionService.setResultStatus(workItem.id, PermanentlyFailed)) mustBe Left(ex)
      }
    }

    "the submission poller pulls the work item" must {
      "submit the request to ETMP successfully" in {
        when(mockPensionSchemeService.registerRacDac(any, any, any)(any, any, any)).thenReturn(Future(Right(Json.obj())))
        await(racDacBulkSubmissionService.submitToETMP(racDacRequest)) mustBe Right(Json.obj())
      }

      "failed submission to ETMP" in {
        val iException = new InternalServerException("Error")
        when(mockPensionSchemeService.registerRacDac(any, any, any)(any, any, any)).thenReturn(Future(Left(iException)))
        await(racDacBulkSubmissionService.submitToETMP(racDacRequest)) mustBe Left(iException)
      }
    }

    "the submission poller queries the queue" must {
      "return the correct status of true if the request is submitted" in {
        when(mockRacDacSubmissionRepo.getTotalNoOfRequestsByPsaId(any)).thenReturn(Future(Right(10L)))
        await(racDacBulkSubmissionService.isRequestSubmitted("test psa id")) mustBe Right(true)
      }

      "return the correct status of false if no request is submitted" in {
        when(mockRacDacSubmissionRepo.getTotalNoOfRequestsByPsaId(any)).thenReturn(Future(Right(0L)))
        await(racDacBulkSubmissionService.isRequestSubmitted("test psa id")) mustBe Right(false)
      }

      "return exception if query is failed" in {
        val ex = new Exception("message")
        when(mockRacDacSubmissionRepo.getTotalNoOfRequestsByPsaId(any)).thenReturn(Future(Left(ex)))
        await(racDacBulkSubmissionService.isRequestSubmitted("test psa id")) mustBe Left(ex)
      }
    }

    "the submission poller queries the queue" must {
      "return none if no request are in the queue" in {
        when(mockRacDacSubmissionRepo.getTotalNoOfRequestsByPsaId(any)).thenReturn(Future(Right(0)))
        when(mockRacDacSubmissionRepo.getNoOfFailureByPsaId(any)).thenReturn(Future(Right(0)))
        await(racDacBulkSubmissionService.isAllFailed("test psa id")) mustBe Right(None)
      }

      "return the correct status of true if all request are failed" in {
        when(mockRacDacSubmissionRepo.getTotalNoOfRequestsByPsaId(any)).thenReturn(Future(Right(10L)))
        when(mockRacDacSubmissionRepo.getNoOfFailureByPsaId(any)).thenReturn(Future(Right(10L)))
        await(racDacBulkSubmissionService.isAllFailed("test psa id")) mustBe Right(Some(true))
      }

      "return the correct status of false if not all requests are failed" in {
        when(mockRacDacSubmissionRepo.getTotalNoOfRequestsByPsaId(any)).thenReturn(Future(Right(10L)))
        when(mockRacDacSubmissionRepo.getNoOfFailureByPsaId(any)).thenReturn(Future(Right(5L)))
        await(racDacBulkSubmissionService.isAllFailed("test psa id")) mustBe Right(Some(false))
      }

      "return exception if there is an error" in {
        when(mockRacDacSubmissionRepo.getTotalNoOfRequestsByPsaId(any)).thenReturn(Future(Right(10L)))
        when(mockRacDacSubmissionRepo.getNoOfFailureByPsaId(any)).thenReturn(Future(Left(new Exception("message"))))
        await(racDacBulkSubmissionService.isAllFailed("test psa id")).left.value.getMessage mustBe "getting query for isAllFailed failed"
      }
    }

    "the submission poller submits a delete request" must {
      "return the correct status of true if all request are deleted" in {
        when(mockRacDacSubmissionRepo.deleteAll(any)).thenReturn(Future(Right(true)))
        await(racDacBulkSubmissionService.deleteAll("test psa id")) mustBe Right(true)
      }

      "return the correct status of false if not all requests are deleted" in {
        when(mockRacDacSubmissionRepo.deleteAll(any)).thenReturn(Future(Right(false)))
        await(racDacBulkSubmissionService.deleteAll("test psa id")) mustBe Right(false)
      }

      "return exception if an error occurred" in {
        val ex = new Exception("message")
        when(mockRacDacSubmissionRepo.deleteAll(any)).thenReturn(Future(Left(ex)))
        await(racDacBulkSubmissionService.deleteAll("test psa id")) mustBe Left(ex)
      }
    }
  }
}
