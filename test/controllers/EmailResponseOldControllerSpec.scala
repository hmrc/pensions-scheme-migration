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

import audit.{AuditService, EmailAuditEvent, EmailAuditEventPsa, StubSuccessfulAuditService}
import base.SpecBase
import models.*
import models.enumeration.JourneyType
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.*
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.PsaId

import java.time.Instant

class EmailResponseOldControllerSpec extends SpecBase {

  val fakeAuditService = new StubSuccessfulAuditService()

  override protected def bindings: Seq[GuiceableModule] = {
    super.bindings ++ Seq(bind[AuditService].to(fakeAuditService))
  }

  private val appCrypto: ApplicationCrypto = injector.instanceOf[ApplicationCrypto]
  private val controller: EmailResponseOldController = injector.instanceOf[EmailResponseOldController]
  private val psa: PsaId = PsaId("A7654321")
  private val pstr = "A0000030"
  private val emailEvents: EmailEvents = EmailEvents(
    Seq(
      EmailEvent(Sent, Instant.now()),
      EmailEvent(Delivered, Instant.now()),
      EmailEvent(Opened, Instant.now())
    )
  )
  private val validJson: JsObject = Json.obj("name" -> "value")

  "retrieveStatus" must {

    "respond OK when given EmailEvents" which {

      JourneyType.values.foreach { eventType =>
        s"will send events excluding Opened for ${eventType.toString} to audit service when psa and pstr passed in url query params" in {
            val encryptedPsa = appCrypto.QueryParameterCrypto.encrypt(PlainText(psa.id)).value
            val encryptedPstr = appCrypto.QueryParameterCrypto.encrypt(PlainText(pstr)).value

            val result = controller.retrieveStatus(eventType, encryptedPsa, encryptedPstr)(fakeRequest.withBody(Json.toJson(emailEvents)))

            status(result) mustBe OK
            fakeAuditService.verifySent(EmailAuditEvent(psa, pstr, eventType, Sent)) mustBe true
            fakeAuditService.verifySent(EmailAuditEvent(psa, pstr, eventType, Delivered)) mustBe true
            fakeAuditService.verifySent(EmailAuditEvent(psa, pstr, eventType, Opened)) mustBe false
        }
      }
    }

    "respond with BAD_REQUEST" when {
      "not given EmailEvents" in {
        fakeAuditService.reset()

        val encryptedPsa = appCrypto.QueryParameterCrypto.encrypt(PlainText(psa.id)).value
        val encryptedPstr = appCrypto.QueryParameterCrypto.encrypt(PlainText(pstr)).value

        val result = controller.retrieveStatus(JourneyType.SCHEME_MIG, encryptedPsa, encryptedPstr)(fakeRequest.withBody(validJson))

        status(result) mustBe BAD_REQUEST
        fakeAuditService.verifyNothingSent() mustBe true

      }
    }

    "respond with FORBIDDEN" when {
      "URL contains an id that does not match PSAID pattern" in {
        fakeAuditService.reset()

        val psa = appCrypto.QueryParameterCrypto.encrypt(PlainText("psa")).value
        val pstr = appCrypto.QueryParameterCrypto.encrypt(PlainText("pstr")).value

        val result = controller.retrieveStatus(JourneyType.RACDAC_IND_MIG, psa, pstr)(fakeRequest.withBody(Json.toJson(emailEvents)))

        status(result) mustBe FORBIDDEN
        contentAsString(result) mustBe "Malformed PSAID"
        fakeAuditService.verifyNothingSent() mustBe true

      }
    }

  }

  "retrieveStatusPsa" must {
    "respond OK when given EmailEvents" which {

      JourneyType.values.foreach { eventType =>
        s"will send events excluding Opened for ${eventType.toString} to audit service" in {
          val encryptedPsa = appCrypto.QueryParameterCrypto.encrypt(PlainText(psa.id)).value

          val result = controller.retrieveStatusPsa(eventType, encryptedPsa)(fakeRequest.withBody(Json.toJson(emailEvents)))

          status(result) mustBe OK
          fakeAuditService.verifySent(EmailAuditEventPsa(psa, eventType, Delivered)) mustBe true
          fakeAuditService.verifySent(EmailAuditEventPsa(psa, eventType, Delivered)) mustBe true
          fakeAuditService.verifySent(EmailAuditEventPsa(psa, eventType, Opened)) mustBe false
        }
      }
    }

    "respond with BAD_REQUEST when not given EmailEvents" in {
      fakeAuditService.reset()

      val encryptedPsa = appCrypto.QueryParameterCrypto.encrypt(PlainText(psa.id)).value

      val result = controller.retrieveStatusPsa(JourneyType.SCHEME_MIG, encryptedPsa)(fakeRequest.withBody(validJson))

      status(result) mustBe BAD_REQUEST
      fakeAuditService.verifyNothingSent() mustBe true
    }

    "respond with FORBIDDEN" when {
      "URL contains an id does not match PSAID pattern" in {
        fakeAuditService.reset()

        val psa = appCrypto.QueryParameterCrypto.encrypt(PlainText("psa")).value

        val result = controller.retrieveStatusPsa(JourneyType.RACDAC_IND_MIG, psa)(fakeRequest.withBody(Json.toJson(emailEvents)))

        status(result) mustBe FORBIDDEN
        contentAsString(result) mustBe "Malformed PSAID"
        fakeAuditService.verifyNothingSent() mustBe true

      }
    }
  }
}
