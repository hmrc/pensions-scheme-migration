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
import connector.SchemeConnector
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, JsResultException, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {

  import SchemeControllerSpec._

  val mockSchemeConnector: SchemeConnector = mock[SchemeConnector]
  val schemeController = new SchemeController(mockSchemeConnector, stubControllerComponents())

  before {
    reset(mockSchemeConnector)
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
        verify(mockSchemeConnector, never()).listOfLegacySchemes(any())(any(), any(), any())
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

}
object SchemeControllerSpec {

  private val validResponse: JsValue = Json.parse("""{
                               |  "totalResults": 2,
                               |  "items": [
                               |    {
                               |      "pstr": "00241768RH",
                               |      "declarationDate": "0001-01-01T00:00:00",
                               |      "schemeName": "THE AMDAIL PENSION SCHEME",
                               |      "schemeOpenDate": "2006-04-05T00:00:00",
                               |      "racDac": false,
                               |      "policyNo": ""
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
