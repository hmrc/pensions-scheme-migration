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

package controllers

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import play.api.libs.json.{JsBoolean, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import service.RacDacBulkSubmissionService
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BulkRacDacControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {

  private val racDacBulkSubmissionService: RacDacBulkSubmissionService = mock[RacDacBulkSubmissionService]
  private val bulkRacDacController = new BulkRacDacController(stubControllerComponents(), racDacBulkSubmissionService)

  before {
    reset(racDacBulkSubmissionService)
  }

  "migrateAllRacDac" must {
    val jsValue = """[{"schemeName":"paul qqq","policyNumber":"24101975"}]""".stripMargin
    val fakeRequest = FakeRequest("POST", "/").withHeaders(("psaId", "A2000001")).withJsonBody(Json.parse(jsValue))

    "return ACCEPTED if all the rac dac requests are successfully pushed to the queue" in {
      when(racDacBulkSubmissionService.enqueue(any())).thenReturn(Future.successful(true))
      val result = bulkRacDacController.migrateAllRacDac(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe ACCEPTED
        verify(racDacBulkSubmissionService, times(1)).enqueue(any())
      }
    }

    "return SERVICE UNAVAILABLE if all the rac dac requests are failed to push to the queue" in {
      when(racDacBulkSubmissionService.enqueue(any())).thenReturn(Future.successful(false))
      val result = bulkRacDacController.migrateAllRacDac(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe SERVICE_UNAVAILABLE
        verify(racDacBulkSubmissionService, times(1)).enqueue(any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      val result = bulkRacDacController.migrateAllRacDac(FakeRequest("GET", "/"))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Missing Body or missing psaId in the header"
        verify(racDacBulkSubmissionService, never).enqueue(any())
      }
    }

    "throw BadRequestException when request is not valid" in {
      val result = bulkRacDacController.migrateAllRacDac(FakeRequest("POST", "/").withHeaders(("psaId", "A2000001")).
        withJsonBody(Json.obj("invalid" -> "request")))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Invalid request received from frontend for rac dac migration"
        verify(racDacBulkSubmissionService, never).enqueue(any())
      }
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

