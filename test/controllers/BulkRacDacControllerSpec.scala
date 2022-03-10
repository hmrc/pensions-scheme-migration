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

package controllers

import audit.{AuditService, RacDacBulkMigrationTriggerAuditEvent}
import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import play.api.libs.json.{JsBoolean, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.RacDacRequestsQueueEventsLogRepository
import service.RacDacBulkSubmissionService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._
import utils.AuthUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BulkRacDacControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {

  private val mockRacDacRequestsQueueEventsLogRepository = mock[RacDacRequestsQueueEventsLogRepository]
  private val mockAuditService = mock[AuditService]
  private val racDacBulkSubmissionService: RacDacBulkSubmissionService = mock[RacDacBulkSubmissionService]
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val authUtil = new AuthUtil(mockAuthConnector, stubControllerComponents())
  private val bulkRacDacController = new BulkRacDacController(stubControllerComponents(),
    racDacBulkSubmissionService, mockAuditService, authUtil, mockRacDacRequestsQueueEventsLogRepository)

  before {
    reset(racDacBulkSubmissionService, mockAuthConnector,mockRacDacRequestsQueueEventsLogRepository)
    when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
      .thenReturn(Future.successful(Some("Ext-137d03b9-d807-4283-a254-fb6c30aceef1")))
    when(mockRacDacRequestsQueueEventsLogRepository.save(any(), any())(any()))
      .thenReturn(Future.successful(true))
  }

  "migrateAllRacDac" must {
    val jsValue =
      """[{"schemeName":"paul qqq","policyNumber":"24101975","pstr":"00615269RH"
        ,"declarationDate":"2012-02-20","schemeOpenDate":"2020-01-01"}]""".stripMargin
    val fakeRequest = FakeRequest("POST", "/").withHeaders(("psaId", "A2000001"),
      HeaderNames.xSessionId -> "123").withJsonBody(Json.parse(jsValue))

    "return ACCEPTED if all the rac dac requests are successfully pushed to the queue and check the audit event" in {
      val captor = ArgumentCaptor.forClass(classOf[RacDacBulkMigrationTriggerAuditEvent])
      doNothing.when(mockAuditService).sendEvent(captor.capture())(any(), any())
      when(racDacBulkSubmissionService.enqueue(any())).thenReturn(Future.successful(true))
      val result = bulkRacDacController.migrateAllRacDac(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe ACCEPTED
        verify(racDacBulkSubmissionService, times(1)).enqueue(any())
      }
      verify(mockAuditService, times(1)).sendEvent(any())(any(),any())
      val expectedAuditEvent = RacDacBulkMigrationTriggerAuditEvent("A2000001", 1, "")
      captor.getValue mustBe expectedAuditEvent

      val jsonCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])

      verify(mockRacDacRequestsQueueEventsLogRepository, times(1)).save(any(), jsonCaptor.capture())(any())
      jsonCaptor.getValue mustBe Json.obj("status" -> ACCEPTED)
    }

    "return SERVICE UNAVAILABLE if all the rac dac requests are failed to push to the queue and check the audit event" in {
      val captor = ArgumentCaptor.forClass(classOf[RacDacBulkMigrationTriggerAuditEvent])
      doNothing.when(mockAuditService).sendEvent(captor.capture())(any(), any())
      when(racDacBulkSubmissionService.enqueue(any())).thenReturn(Future.successful(false))
      val result = bulkRacDacController.migrateAllRacDac(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe SERVICE_UNAVAILABLE
        verify(racDacBulkSubmissionService, times(1)).enqueue(any())
      }
      val expectedAuditEvent = RacDacBulkMigrationTriggerAuditEvent("A2000001", 0, "Queue Service Unavailable")
      captor.getValue mustBe expectedAuditEvent
    }

    "throw BadRequestException when PSAId is not present in the header and check the audit event" in {
      val captor = ArgumentCaptor.forClass(classOf[RacDacBulkMigrationTriggerAuditEvent])
      doNothing.when(mockAuditService).sendEvent(captor.capture())(any(), any())
      val result = bulkRacDacController.migrateAllRacDac(FakeRequest("GET", "/"))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Missing Body or missing psaId in the header"
        verify(racDacBulkSubmissionService, never).enqueue(any())
      }
      val expectedAuditEvent = RacDacBulkMigrationTriggerAuditEvent("", 0, "Missing Body or missing psaId in the header")
      captor.getValue mustBe expectedAuditEvent
    }

    "throw BadRequestException when request is not valid and check the audit event" in {
      val captor = ArgumentCaptor.forClass(classOf[RacDacBulkMigrationTriggerAuditEvent])
      doNothing.when(mockAuditService).sendEvent(captor.capture())(any(), any())
      val result = bulkRacDacController.migrateAllRacDac(FakeRequest("POST", "/").withHeaders(("psaId", "A2000001")).
        withJsonBody(Json.obj("invalid" -> "request")))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Invalid request received from frontend for rac dac migration"
        verify(racDacBulkSubmissionService, never).enqueue(any())
      }
      val expectedAuditEvent = RacDacBulkMigrationTriggerAuditEvent("A2000001", 0, "Invalid request received from frontend for rac dac migration")
      captor.getValue mustBe expectedAuditEvent
    }
  }

  "isRequestSubmitted" must {
    val fakeRequest = FakeRequest("GET", "/").withHeaders(("psaId", "A2000001"))

    "return OK with true if some request is in the queue" in {
      when(racDacBulkSubmissionService.isRequestSubmitted(any())).thenReturn(Future.successful(Right(true)))
      val result = bulkRacDacController.isRequestSubmitted(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsBoolean(true)
        verify(racDacBulkSubmissionService, times(1)).isRequestSubmitted(any())
      }
    }

    "return OK with false if no request is in the queue" in {
      when(racDacBulkSubmissionService.isRequestSubmitted(any())).thenReturn(Future.successful(Right(false)))
      val result = bulkRacDacController.isRequestSubmitted(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsBoolean(false)
        verify(racDacBulkSubmissionService, times(1)).isRequestSubmitted(any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      val result = bulkRacDacController.isRequestSubmitted(FakeRequest("GET", "/"))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Missing psaId in the header"
        verify(racDacBulkSubmissionService, never).isRequestSubmitted(any())
      }
    }

    "return SERVICE UNAVAILABLE if there is an error occurred while querying" in {
      when(racDacBulkSubmissionService.isRequestSubmitted(any())).thenReturn(Future.successful(Left(new Exception("message"))))
      val result = bulkRacDacController.isRequestSubmitted(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe SERVICE_UNAVAILABLE
        verify(racDacBulkSubmissionService, times(1)).isRequestSubmitted(any())
      }
    }
  }

  "isAllFailed" must {
    val fakeRequest = FakeRequest("GET", "/").withHeaders(("psaId", "A2000001"))

    "return No Content if there is no requests in the queue" in {
      when(racDacBulkSubmissionService.isAllFailed(any())).thenReturn(Future.successful(Right(None)))
      val result = bulkRacDacController.isAllFailed(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe NO_CONTENT
        verify(racDacBulkSubmissionService, times(1)).isAllFailed(any())
      }
    }

    "return OK with true if all requests in the queue is failed" in {
      when(racDacBulkSubmissionService.isAllFailed(any())).thenReturn(Future.successful(Right(Some(true))))
      val result = bulkRacDacController.isAllFailed(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsBoolean(true)
        verify(racDacBulkSubmissionService, times(1)).isAllFailed(any())
      }
    }

    "return OK with false if some requests in the queue is not failed" in {
      when(racDacBulkSubmissionService.isAllFailed(any())).thenReturn(Future.successful(Right(Some(false))))
      val result = bulkRacDacController.isAllFailed(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsBoolean(false)
        verify(racDacBulkSubmissionService, times(1)).isAllFailed(any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      val result = bulkRacDacController.isAllFailed(FakeRequest("GET", "/"))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Missing psaId in the header"
        verify(racDacBulkSubmissionService, never).isAllFailed(any())
      }
    }

    "return SERVICE_UNAVAILABLE if there is an error occurred" in {
      when(racDacBulkSubmissionService.isAllFailed(any())).thenReturn(Future.successful(Left(new Exception("message"))))
      val result = bulkRacDacController.isAllFailed(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe SERVICE_UNAVAILABLE
        verify(racDacBulkSubmissionService, times(1)).isAllFailed(any())
      }
    }
  }

  "deleteAll" must {
    val fakeRequest = FakeRequest("DELETE", "/").withHeaders(("psaId", "A2000001"))

    "return OK with true if all requests in the queue is deleted" in {
      when(racDacBulkSubmissionService.deleteAll(any())).thenReturn(Future.successful(Right(true)))
      val result = bulkRacDacController.deleteAll(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsBoolean(true)
        verify(racDacBulkSubmissionService, times(1)).deleteAll(any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      val result = bulkRacDacController.deleteAll(FakeRequest("DELETE", "/"))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Missing psaId in the header"
        verify(racDacBulkSubmissionService, never).deleteAll(any())
      }
    }

    "return SERVICE_UNAVAILABLE if there is an error occurred" in {
      when(racDacBulkSubmissionService.deleteAll(any())).thenReturn(Future.successful(Left(new Exception("message"))))
      val result = bulkRacDacController.deleteAll(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe SERVICE_UNAVAILABLE
        verify(racDacBulkSubmissionService, times(1)).deleteAll(any())
      }
    }
  }
}

