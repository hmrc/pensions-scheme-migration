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

package connector

import base.WireMockHelper
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, post, urlEqualTo}
import models.SendEmailRequest
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.guice.GuiceableModule
import play.api.inject.{Injector, bind}
import play.api.libs.json.Json
import repositories._
import uk.gov.hmrc.http.HeaderCarrier

class EmailConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with MockitoSugar {
  protected def portConfigKey: String = "microservice.services.email.port"

  val url: String = "/hmrc/email"

  val email: SendEmailRequest =
    SendEmailRequest(
      to = List("test@test.com"),
      templateId = "test-template-id",
      parameters = Map(
        "test-param1" -> "test-value1",
        "test-param2" -> "test-value2"
      ),
      force = false,
      eventUrl = "test-response-url"
    )

  val emailJson: String = Json.stringify(Json.toJson(email))

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def connector(injector: Injector): EmailConnector = injector.instanceOf[EmailConnector]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
      bind[ListOfLegacySchemesCacheRepository].toInstance(mock[ListOfLegacySchemesCacheRepository]),
      bind[LockCacheRepository].toInstance(mock[LockCacheRepository]),
      bind[RacDacRequestsQueueEventsLogRepository].toInstance(mock[RacDacRequestsQueueEventsLogRepository]),
      bind[RacDacRequestsQueueRepository].toInstance(mock[RacDacRequestsQueueRepository]),
      bind[SchemeDataCacheRepository].toInstance(mock[SchemeDataCacheRepository])
    )

  "EmailConnector" must "return EmailSent when an email request is accepted by the API" in {

    server.stubFor(
      post(urlEqualTo(url))
        .withRequestBody(equalToJson(emailJson))
        .willReturn(
          aResponse()
            .withStatus(Status.ACCEPTED)
        )
    )

    connector(app.injector).sendEmail(email.to.head, email.templateId, email.parameters, email.eventUrl) map {
      response =>
        response mustBe EmailSent
    }

  }

  it must "return EmailNotSent when any other non-error response is returned bu the API" in {

    server.stubFor(
      post(urlEqualTo(url))
        .withRequestBody(equalToJson(emailJson))
        .willReturn(
          aResponse()
            .withStatus(Status.NO_CONTENT)
        )
    )

    connector(app.injector).sendEmail(email.to.head, email.templateId, email.parameters, email.eventUrl) map {
      response =>
        response mustBe EmailNotSent
    }

  }

  it must "return EmailNotSent when an error response is returned by the API" in {

    server.stubFor(
      post(urlEqualTo(url))
        .withRequestBody(equalToJson(emailJson))
        .willReturn(
          aResponse()
            .withStatus(Status.INTERNAL_SERVER_ERROR)
        )
    )

    connector(app.injector).sendEmail(email.to.head, email.templateId, email.parameters, email.eventUrl) map {
      response =>
        response mustBe EmailNotSent
    }

  }
}
