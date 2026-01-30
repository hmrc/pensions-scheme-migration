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

package controllers

import base.SpecBase
import models.Scheme
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, BodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.ListOfLegacySchemesCacheRepository
import service.PensionSchemeService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._
import utils.AuthUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {

  import SchemeControllerSpec._

  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockPensionSchemeService: PensionSchemeService = mock[PensionSchemeService]
  private val mockListOfLegacySchemesCacheRepository: ListOfLegacySchemesCacheRepository = mock[ListOfLegacySchemesCacheRepository]
  private val schemeController = new SchemeController(mockPensionSchemeService,
    mockListOfLegacySchemesCacheRepository, stubControllerComponents(), new actions.AuthAction(mockAuthConnector, app.injector.instanceOf[BodyParsers.Default]))
  private val psaId = AuthUtils.psaId

  before {
    reset(mockAuthConnector)
    reset(mockPensionSchemeService)
    reset(mockListOfLegacySchemesCacheRepository)
    AuthUtils.authStub(mockAuthConnector)
    when(mockPensionSchemeService.getListOfLegacySchemes(ArgumentMatchers.eq(psaId))(any())).thenReturn(
      Future.successful(Right(validResponse))
    )
  }

  "list of legacy schemes" must {
    val fakeRequest = FakeRequest("GET", "/")

    "return OK with list of schemes for PSA and cache has not value exists " in {
      when(mockListOfLegacySchemesCacheRepository.get(any())(any()))
        .thenReturn(Future.successful {
          None
        })
      when(mockListOfLegacySchemesCacheRepository.upsert(any(), any())(any()))
        .thenReturn(Future.successful(true))
      val result = schemeController.listOfLegacySchemes(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual transformedResponse
      }
    }
    "return OK with list of schemes for PSA and cache has value exists " in {
      when(mockListOfLegacySchemesCacheRepository.get(any())(any()))
        .thenReturn(Future.successful {
          Some(validResponse)
        })
      val result = schemeController.listOfLegacySchemes(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual transformedResponse
      }
    }

    "throw JsResultException when the invalid data returned from If/ETMP" in {
      when(mockListOfLegacySchemesCacheRepository.get(any())(any()))
        .thenReturn(Future.successful {
          None
        })
      when(mockPensionSchemeService.getListOfLegacySchemes(ArgumentMatchers.eq(psaId))(any())).thenReturn(
        Future.failed(JsResultException(Seq()))
      )
      when(mockListOfLegacySchemesCacheRepository.upsert(any(), any())(any()))
        .thenReturn(Future.successful(true))
      val result = schemeController.listOfLegacySchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[JsResultException]
      }
    }

    "return Bad Request when bad request returned from If" in {
      val invalidPayload: JsObject = Json.obj(
        "code" -> "INVALID_PSAID",
        "reason" -> "Submission has not passed validation. Invalid parameter PSAID."
      )

      when(mockPensionSchemeService.getListOfLegacySchemes(ArgumentMatchers.eq(psaId))(any())).thenReturn(
        Future.successful(Left(new HttpException(Json.stringify(invalidPayload), 400)))
      )

      when(mockListOfLegacySchemesCacheRepository.get(any())(any()))
        .thenReturn(Future.successful {
          None
        })
      val result = schemeController.listOfLegacySchemes(fakeRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe Json.stringify(invalidPayload)
    }

    "return Internal Server Error when UpStream5XXResponse returned" in {
      val serviceUnavailable: JsObject = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )

      when(mockPensionSchemeService.getListOfLegacySchemes(ArgumentMatchers.eq(psaId))(any())).thenReturn(
        Future.successful(Left(new HttpException(Json.stringify(serviceUnavailable), 500)))
      )

      when(mockListOfLegacySchemesCacheRepository.get(any())(any()))
        .thenReturn(Future.successful {
          None
        })
      val result = schemeController.listOfLegacySchemes(fakeRequest)
      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe Json.stringify(serviceUnavailable)
    }

    "Return the error when any other exception returned from If" in {
      when(mockListOfLegacySchemesCacheRepository.get(any())(any()))
        .thenReturn(Future.successful {
          None
        })

      when(mockPensionSchemeService.getListOfLegacySchemes(ArgumentMatchers.eq(psaId))(any())).thenReturn(
        Future.successful(Left(new HttpException("{}", 403)))
      )

      when(mockListOfLegacySchemesCacheRepository.upsert(any(), any())(any()))
        .thenReturn(Future.successful(true))
      val result = schemeController.listOfLegacySchemes(fakeRequest)
      status(result) mustBe FORBIDDEN
      contentAsString(result) mustBe "{}"
    }
  }

  "registerScheme" must {

    def fakeRequest(data: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/").withJsonBody(data).withHeaders(("psaId", psaId))

    val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
    "return Locked when the scheme is already registered by the user within the TTL" in {
      when(mockPensionSchemeService.registerScheme(any(), any())(any(), any()))
        .thenReturn(Future.successful(Left(LockedException("locked"))))

      val result = schemeController.registerScheme(Scheme)(fakeRequest(validData))
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe LOCKED
      }
    }

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

    "return exception when incorrect data type returned from api" in {
      when(mockPensionSchemeService.registerScheme(any(), any())(any(), any())).thenReturn(
        Future.failed(HttpException("response body", 200)))

      val result = schemeController.registerScheme(Scheme)(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "response body"
      }
    }
  }


  "removeListOfLegacySchemesCache" must {

    "return OK when the scheme is registered successfully" in {
      when(mockListOfLegacySchemesCacheRepository.remove(any())(any())).thenReturn(
        Future.successful(true))
      val fakeRequest = FakeRequest("GET", "/").withHeaders(("psaId", psaId))
      val result = schemeController.removeListOfLegacySchemesCache(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
      }
    }
  }
}

object SchemeControllerSpec {

  private val validResponse: JsValue = Json.parse(
    """{
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

  private val transformedResponse = Json.parse(
    """{
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
