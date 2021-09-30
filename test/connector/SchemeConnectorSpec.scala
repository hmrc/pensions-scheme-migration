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

import audit.{AuditService, ListOfLegacySchemesAuditEvent}
import base.WireMockHelper
import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doNothing
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest._
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{UpstreamErrorResponse, HeaderCarrier}
import org.mockito.Matchers.any

class SchemeConnectorSpec
  extends AsyncFlatSpec
    with WireMockHelper
    with OptionValues
    with MockitoSugar
    with RecoverMethods
    with EitherValues {

  import SchemeConnectorSpec._

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  override protected def portConfigKey: String = "microservice.services.if-hod.port"

  private val mockAuditService = mock[AuditService]

  def connector: SchemeConnector = app.injector.instanceOf[SchemeConnector]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(mockAuditService)
    )

  "SchemeConnector listOfScheme" should "return OK with the list of schemes response for PSA and audit the response" in {
    val captor = ArgumentCaptor.forClass(classOf[ListOfLegacySchemesAuditEvent])
    doNothing().when(mockAuditService).sendEvent(captor.capture())(any(), any())
    server.stubFor(
      get(listOfSchemesIFUrl)
        .willReturn(
          ok(Json.stringify(validListOfSchemeIFResponse))
        )
    )

    connector.listOfLegacySchemes(idValue).map { response =>
      response.right.value shouldBe validListOfSchemeIFResponse
      val expectedAuditEvent = ListOfLegacySchemesAuditEvent(OK, 2, "")
      captor.getValue shouldBe expectedAuditEvent
    }
  }

  it should "return 422 when if/ETMP throws Unprocessable Entity and audit the response" in {
    val responseBody = "test response body"
    val captor = ArgumentCaptor.forClass(classOf[ListOfLegacySchemesAuditEvent])
    doNothing().when(mockAuditService).sendEvent(captor.capture())(any(), any())
    server.stubFor(
      get(listOfSchemesIFUrl)
        .willReturn(
          aResponse()
            .withStatus(422)
            .withBody(responseBody)
        )
    )
    connector.listOfLegacySchemes(idValue).map { response =>
      response.left.value.responseCode shouldBe UNPROCESSABLE_ENTITY
      val expectedAuditEvent = ListOfLegacySchemesAuditEvent(UNPROCESSABLE_ENTITY, 0, responseBody)
      captor.getValue shouldBe expectedAuditEvent
    }
  }

  it should "throw UpStream5XXResponse when if/ETMP throws Server error" in {
    doNothing().when(mockAuditService).sendEvent(any())(any(), any())
    server.stubFor(
      get(listOfSchemesIFUrl)
        .willReturn(
          serverError()
        )
    )

    recoverToExceptionIf[UpstreamErrorResponse] {
      connector.listOfLegacySchemes(idValue)
    } map {
      _.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

}

object SchemeConnectorSpec {
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  private val idValue = "test"
  private val validListOfSchemeIFResponse = Json.parse(
    """
      |{
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
      |}
      |""".stripMargin)

  private val listOfSchemesIFUrl: String =
    s"/pension-schemes/schemes/$idValue"
}

