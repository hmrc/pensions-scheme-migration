/*
 * Copyright 2025 HM Revenue & Customs
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

import audit.{AuditService, EmailAuditEvent, StubSuccessfulAuditService}
import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import models.{Delivered, EmailEvent, EmailEvents, EmailIdentifiers, Opened, Sent}
import models.enumeration.JourneyType
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, defaultAwaitTimeout, route, status, writeableOf_AnyContentAsJson}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.Instant

class EmailResponseControllerISpec
  extends AnyWordSpec
  with GuiceOneAppPerSuite
  with WireMockSupport
  with Matchers {

  val fakeAuditService = new StubSuccessfulAuditService()
  val psa: PsaId = PsaId("A7654321")
  val pstr = "A0000030"
  val emailEvents: EmailEvents = EmailEvents(Seq(EmailEvent(Sent, Instant.now()), EmailEvent(Delivered, Instant.now()), EmailEvent(Opened, Instant.now())))
  val emailIdentifiers: EmailIdentifiers = EmailIdentifiers(Some(psa.id), Some(pstr))
  val emailIdentifiersNoPsa: EmailIdentifiers = EmailIdentifiers(None, Some(pstr))
  val emailIdentifiersNoPtsr: EmailIdentifiers = EmailIdentifiers(Some(psa.id), None)


  override def fakeApplication(): Application = GuiceApplicationBuilder().configure(
    "microservice.services.auth.port" -> wireMockServer.port(),
    "microservice.services.email.port" -> wireMockServer.port(),
    "microservice.services.if-hod.port" -> wireMockServer.port()
  ).overrides(
    bind[AuditService].toInstance(fakeAuditService)
  ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          ok("{}")
        )
    )
  }

  startWireMock()

  val crypto = new ApplicationCrypto(fakeApplication().configuration.underlying)
  val encryptedPsa: String = crypto.QueryParameterCrypto.encrypt(PlainText(psa.id)).value
  val encryptedPstr: String = crypto.QueryParameterCrypto.encrypt(PlainText(pstr)).value

  "retrieveStatus" must {

    "respond OK when given EmailEvents" which {
      JourneyType.values.foreach { eventType =>
        s"will send events excluding Opened for ${eventType.toString} to audit service when psa and pstr passed in url query params" in {
          val controllerUrl = s"/pensions-scheme-migration/email-response/$eventType/$encryptedPsa/$encryptedPstr"

          val request = FakeRequest(POST, controllerUrl)
            .withJsonBody(Json.toJson(emailEvents))
            .withHeaders(("Authorization", "Bearer token"))
          val result = route(fakeApplication(), request)

          result.map(status) mustBe Some(OK)
          fakeAuditService.verifySent(EmailAuditEvent(psa, pstr, eventType, Sent)) mustBe true
          fakeAuditService.verifySent(EmailAuditEvent(psa, pstr, eventType, Delivered)) mustBe true
          fakeAuditService.verifySent(EmailAuditEvent(psa, pstr, eventType, Opened)) mustBe false
        }

        s"will send events excluding Opened for ${eventType.toString} to audit service when psa and pstr NOT passed in url but in request body" in {
          val controllerUrl = s"/pensions-scheme-migration/email-response/$eventType"

          val request = FakeRequest(POST, controllerUrl)
            .withJsonBody(Json.toJson(Json.toJsObject(emailEvents) ++ Json.toJsObject(emailIdentifiers)))
            .withHeaders(("Authorization", "Bearer token"))
          val result = route(fakeApplication(), request)

          result.map(status) mustBe Some(OK)
          fakeAuditService.verifySent(EmailAuditEvent(psa, pstr, eventType, Sent)) mustBe true
          fakeAuditService.verifySent(EmailAuditEvent(psa, pstr, eventType, Delivered)) mustBe true
          fakeAuditService.verifySent(EmailAuditEvent(psa, pstr, eventType, Opened)) mustBe false
        }
      }
    }
  }
}
