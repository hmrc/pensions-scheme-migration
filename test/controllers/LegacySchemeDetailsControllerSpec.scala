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
import connector.LegacySchemeDetailsConnector
import connector.LegacySchemeDetailsConnectorSpec.readJsonFromFile
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{BadRequestException, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LegacySchemeDetailsControllerSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfter
    with PatienceConfiguration {


  private val mockSchemeConnector: LegacySchemeDetailsConnector = mock[LegacySchemeDetailsConnector]
  private val schemeDetailsController =
    new LegacySchemeDetailsController(mockSchemeConnector, stubControllerComponents())
  private val pstr = "00000000AA"
  private val psaId = "000"
  private val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswers.json")

  before {
    reset(mockSchemeConnector)
  }

  "getLegacySchemeDetails" must {

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", "/").withHeaders(
        ("pstr", pstr),
        ("PSAId", psaId)
      )

    "return OK when the scheme is registered successfully" in {

      val successResponse = userAnswersResponse
      when(mockSchemeConnector.getSchemeDetails(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(successResponse.as[JsObject])))

      val result = schemeDetailsController.getLegacySchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustBe successResponse
        verify(mockSchemeConnector, times(1)).getSchemeDetails(any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when psaId is not present in the header" in {
      val result = schemeDetailsController.getLegacySchemeDetails()(FakeRequest("GET", "/").withHeaders(
        ("pstr", pstr)
      ))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters PSAId or PSTR"
        verify(mockSchemeConnector, never).getSchemeDetails(any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when pstr is not present in the header" in {

      val result = schemeDetailsController.getLegacySchemeDetails()(FakeRequest("GET", "/").withHeaders(
        ("PSAId", psaId)
      ))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters PSAId or PSTR"
        verify(mockSchemeConnector, never).getSchemeDetails(any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when bad request with INVALID_DATA returned from If" in {
      when(mockSchemeConnector.getSchemeDetails(any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_DATA")))
        )

      val result = schemeDetailsController.getLegacySchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_DATA")
      }
    }

    "throw BadRequestException when bad request returned from If" in {
      val invalidPayload: JsObject = Json.obj(
        "code" -> "INVALID_PSAID",
        "reason" -> "Submission has not passed validation. Invalid parameter PSAID."
      )
      when(mockSchemeConnector.getSchemeDetails(any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new BadRequestException(invalidPayload.toString())))

      val result = schemeDetailsController.getLegacySchemeDetails(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe invalidPayload.toString()
        verify(mockSchemeConnector, times(1)).getSchemeDetails(any(), any())(any(), any(), any())
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any())(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), NOT_FOUND, NOT_FOUND))
      )

      val result = schemeDetailsController.getLegacySchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with SERVICE_UNAVAILABLE returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE))
        )

      val result = schemeDetailsController.getLegacySchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with INTERNAL_SERVER_ERROR returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
        )

      val result = schemeDetailsController.getLegacySchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw generic exception when any other exception returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = schemeDetailsController.getLegacySchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
      }
    }
  }
}