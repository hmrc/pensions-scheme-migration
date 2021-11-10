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
import connector.LegacySchemeDetailsConnectorSpec.readJsonFromFile
import connector.SchemeConnector
import models.Scheme
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import play.api.libs.json.{JsObject, JsResultException, JsValue, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import service.PensionSchemeService
import uk.gov.hmrc.http._

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {

  import SchemeControllerSpec._

  val mockSchemeConnector: SchemeConnector = mock[SchemeConnector]
  val mockPensionSchemeService: PensionSchemeService = mock[PensionSchemeService]
  val schemeController = new SchemeController(mockSchemeConnector,mockPensionSchemeService, stubControllerComponents())

  before {
    reset(mockSchemeConnector)
    reset(mockPensionSchemeService)
  }

  "list of legacy schemes" must {
    val fakeRequest = FakeRequest("GET", "/").withHeaders(("psaId", "A2000001"))

    "return OK with list of schemes for PSA when If/ETMP returns it successfully" in {
      when(mockSchemeConnector.listOfLegacySchemes(meq("A2000001"))(any(), any(), any()))
        .thenReturn(Future.successful(Right(validResponse)))
      val result = schemeController.listOfLegacySchemes(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual transformedResponse
        verify(mockSchemeConnector, times(1)).listOfLegacySchemes(any())(any(), any(), any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      val result = schemeController.listOfLegacySchemes(FakeRequest("GET", "/"))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing PSAId"
        verify(mockSchemeConnector, never).listOfLegacySchemes(any())(any(), any(), any())
      }
    }

    "throw JsResultException when the invalid data returned from If/ETMP" in {
      val invalidResponse = Json.obj("invalid" -> "data")
      when(mockSchemeConnector.listOfLegacySchemes(meq("A2000001"))(any(), any(), any()))
        .thenReturn(Future.successful(Right(invalidResponse)))
      val result = schemeController.listOfLegacySchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[JsResultException]
        verify(mockSchemeConnector, times(1)).listOfLegacySchemes(any())(any(), any(), any())
      }
    }

    "throw BadRequestException when bad request returned from If" in {
      val invalidPayload: JsObject = Json.obj(
        "code" -> "INVALID_PSAID",
        "reason" -> "Submission has not passed validation. Invalid parameter PSAID."
      )
      when(mockSchemeConnector.listOfLegacySchemes(meq("A2000001"))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(invalidPayload.toString())))

      val result = schemeController.listOfLegacySchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe invalidPayload.toString()
        verify(mockSchemeConnector, times(1)).listOfLegacySchemes(meq("A2000001"))(any(), any(), any())
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse returned" in {
      val serviceUnavailable: JsObject = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )
      when(mockSchemeConnector.listOfLegacySchemes(meq("A2000001"))(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(serviceUnavailable.toString(), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = schemeController.listOfLegacySchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe serviceUnavailable.toString()
        verify(mockSchemeConnector, times(1)).listOfLegacySchemes(meq("A2000001"))(any(), any(), any())
      }
    }

    "throw generic exception when any other exception returned from If" in {
      when(mockSchemeConnector.listOfLegacySchemes(meq("A2000001"))(any(), any(), any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = schemeController.listOfLegacySchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
        verify(mockSchemeConnector, times(1)).listOfLegacySchemes(meq("A2000001"))(any(), any(), any())
      }
    }
  }

  "registerScheme" must {

    def fakeRequest(data: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/").withJsonBody(data).withHeaders(("psaId", "A2000001"))

    "return OK when the scheme is registered successfully" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now, "schemeReferenceNumber" -> "S0123456789")
      when(mockPensionSchemeService.registerScheme(any(), meq(validData))(any(), any())).thenReturn(
        Future.successful(Right(successResponse)))

      val result = schemeController.registerScheme(Scheme)(fakeRequest(validData))
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustBe successResponse
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")

      val result = schemeController.registerScheme(Scheme)(FakeRequest("POST", "/").withJsonBody(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request without PSAId or request body"
        verify(mockPensionSchemeService, never).registerScheme(any(),
          any())(any(), any())
      }
    }

    "throw BadRequestException when bad request returned from If" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val invalidPayload: JsObject = Json.obj(
        "code" -> "INVALID_PAYLOAD",
        "reason" -> "Submission has not passed validation. Invalid PAYLOAD"
      )
      when(mockPensionSchemeService.registerScheme(any(), any())(any(), any())).thenReturn(
        Future.failed(new BadRequestException(invalidPayload.toString())))

      val result = schemeController.registerScheme(Scheme)(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe invalidPayload.toString()
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from If" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val invalidSubmission: JsObject = Json.obj(
        "code" -> "INVALID_SUBMISSION",
        "reason" -> "Duplicate submission acknowledgement reference from remote endpoint returned."
      )
      when(mockPensionSchemeService.registerScheme(any(), any())(any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(invalidSubmission.toString(), CONFLICT, CONFLICT)))

      val result = schemeController.registerScheme(Scheme)(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe invalidSubmission.toString()
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse returned from If" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val serviceUnavailable: JsObject = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )
      when(mockPensionSchemeService.registerScheme(any(), any())(any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(serviceUnavailable.toString(), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = schemeController.registerScheme(Scheme)(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe serviceUnavailable.toString()
      }
    }

    "throw generic exception when any other exception returned from If" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      when(mockPensionSchemeService.registerScheme(any(), any())(any(), any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = schemeController.registerScheme(Scheme)(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
      }
    }
  }

}
object SchemeControllerSpec {

  private val validResponse: JsValue = Json.parse("""{
                               |  "totalResults": 2,
                               |  "items": [
                               |    {
                               |      "pstr": "00241768RH",
                               |      "relationshipStartDate": "0001-01-01T00:00:00",
                               |      "schemeName": "THE AMDAIL PENSION SCHEME",
                               |      "schemeOpenDate": "2006-04-05T00:00:00",
                               |      "racDac": false,
                               |      "policyNumber": ""
                               |    },
                               |    {
                               |      "pstr": "00615269RH",
                               |      "relationshipStartDate": "2012-02-20T00:00:00",
                               |      "schemeName": "paul qqq",
                               |      "schemeOpenDate": "paul qqq",
                               |      "racDac": true,
                               |      "policyNumber": "24101975"
                               |    }
                               |  ]
                               |}""".stripMargin)

  private val transformedResponse = Json.parse("""{
                                                 |  "totalResults": 2,
                                                 |  "items": [
                                                 |    {
                                                 |      "pstr": "00241768RH",
                                                 |      "declarationDate": "0001-01-01T00:00:00",
                                                 |      "schemeName": "THE AMDAIL PENSION SCHEME",
                                                 |      "schemeOpenDate": "2006-04-05T00:00:00",
                                                 |      "racDac": false
                                                 |    },
                                                 |    {
                                                 |      "pstr": "00615269RH",
                                                 |      "declarationDate": "2012-02-20T00:00:00",
                                                 |      "schemeName": "paul qqq",
                                                 |      "schemeOpenDate": "paul qqq",
                                                 |      "racDac": true,
                                                 |      "policyNo": "24101975"
                                                 |    }
                                                 |  ]
                                                 |}""".stripMargin)

}
