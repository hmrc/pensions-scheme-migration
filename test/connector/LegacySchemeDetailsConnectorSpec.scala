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

package connector

import audit.{AuditService, LegacySchemeDetailsAuditEvent, ListOfLegacySchemesAuditEvent}
import base.{JsonFileReader, WireMockHelper}
import com.github.tomakehurst.wiremock.client.WireMock._
import connector.SchemeConnectorSpec.idValue
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, OK, UNPROCESSABLE_ENTITY}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, UpstreamErrorResponse}


class LegacySchemeDetailsConnectorSpec
  extends AsyncFlatSpec
    with WireMockHelper
    with OptionValues
    with MockitoSugar
    with RecoverMethods
    with EitherValues {

  import LegacySchemeDetailsConnectorSpec._

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  override protected def portConfigKey: String = "microservice.services.if-hod.port"

  def connector: LegacySchemeDetailsConnector = app.injector.instanceOf[LegacySchemeDetailsConnector]

  private val mockAuditService = mock[AuditService]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(mockAuditService)
    )

  "LegacySchemeDetailsConnector getLegacySchemeDetails" should "return user answer json" in {
    val captor = ArgumentCaptor.forClass(classOf[LegacySchemeDetailsAuditEvent])
    doNothing.when(mockAuditService).sendEvent(captor.capture())(any(), any())
    val IfResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(IfResponse.toString())
        )
    )
    connector.getSchemeDetails(psaId, pstr).map { response =>
      response.right.value shouldBe userAnswersResponse
    }
  }

  it should "return a BadRequestException for a 400 INVALID_ID response" in {
    val captor = ArgumentCaptor.forClass(classOf[LegacySchemeDetailsAuditEvent])
    doNothing.when(mockAuditService).sendEvent(captor.capture())(any(), any())
    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_ID"))
        )
    )

    recoverToExceptionIf[BadRequestException] {
      connector.getSchemeDetails(psaId, pstr)
    } map {
      _.responseCode shouldBe BAD_REQUEST
    }
  }

  it should "throw Upstream4XX for Not Found - 404" in {
    val captor = ArgumentCaptor.forClass(classOf[LegacySchemeDetailsAuditEvent])
    doNothing.when(mockAuditService).sendEvent(captor.capture())(any(), any())
    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          notFound
            .withBody(errorResponse("NOT_FOUND"))
        )
    )
    recoverToExceptionIf[UpstreamErrorResponse] {
      connector.getSchemeDetails(psaId, pstr)
    } map {
      _.statusCode shouldBe NOT_FOUND
    }
  }

  it should "throw Upstream4XX for server unavailable - 403" in {
    val captor = ArgumentCaptor.forClass(classOf[LegacySchemeDetailsAuditEvent])
    doNothing.when(mockAuditService).sendEvent(captor.capture())(any(), any())
    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          forbidden
            .withBody(errorResponse("FORBIDDEN"))
        )
    )

    recoverToExceptionIf[UpstreamErrorResponse] {
      connector.getSchemeDetails(psaId, pstr)
    } map {
      _.statusCode shouldBe FORBIDDEN
    }
  }

  it should "send audit event for successful response" in {
    val captor = ArgumentCaptor.forClass(classOf[LegacySchemeDetailsAuditEvent])
    doNothing.when(mockAuditService).sendEvent(captor.capture())(any(), any())
    val IfResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(IfResponse.toString())
        )
    )
    connector.getSchemeDetails(psaId, pstr).map { _ =>
      val expectedAuditEvent = LegacySchemeDetailsAuditEvent(psaId, pstr, OK, Some(userAnswersResponse))
      captor.getValue shouldBe expectedAuditEvent
    }
  }

  it should "return 422 when if/TPSS throws Unprocessable Entity and audit the response" in {
    val captor = ArgumentCaptor.forClass(classOf[LegacySchemeDetailsAuditEvent])
    doNothing.when(mockAuditService).sendEvent(captor.capture())(any(), any())

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          badRequestEntity
          .withHeader("Content-Type", "application/json")
          .withBody(errorResponse("UNPROCESSABLE_ENTITY"))
        )
    )

      connector.getSchemeDetails(psaId, pstr).map { response =>
        response.left.value.responseCode shouldBe UNPROCESSABLE_ENTITY
        val expectedAuditEvent = LegacySchemeDetailsAuditEvent(psaId, pstr, UNPROCESSABLE_ENTITY, None)
        captor.getValue shouldBe expectedAuditEvent
    }
  }
}


object LegacySchemeDetailsConnectorSpec extends JsonFileReader {
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  private val pstr = "20010010AA"

  private val psaId = "psa-id"

  val schemeDetailsIFUrl: String = s"/pension-schemes/schemes/$pstr/GetSchemeDetails?psaId=$psaId"

  private def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }
}



