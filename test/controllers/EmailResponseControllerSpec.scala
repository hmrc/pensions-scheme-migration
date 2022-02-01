/*
 * Copyright 2022 HM Revenue & Customs
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
import base.SpecBase
import controllers.EmailResponseControllerSpec.psa
import models._
import models.enumeration.JourneyType
import org.joda.time.DateTime
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.PsaId

class EmailResponseControllerSpec extends SpecBase {

  import EmailResponseControllerSpec._

  "EmailResponseController" must {

    "respond OK when given EmailEvents" which {

      JourneyType.values.foreach { eventType =>
        s"will send events excluding Opened for ${eventType.toString} to audit service" in {

          running(_.overrides(
            bind[AuditService].to(fakeAuditService)
          )) { app =>

            val encryptedPsa = app.injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(psa.id)).value
            val encryptedPstr = app.injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(pstr)).value
            val controller = app.injector.instanceOf[EmailResponseController]

            val result = controller.retrieveStatus(eventType,encryptedPsa,encryptedPstr)(fakeRequest.withBody(Json.toJson(emailEvents)))

            status(result) mustBe OK
            fakeAuditService.verifySent(EmailAuditEvent(psa, pstr,eventType, Sent)) mustBe true
            fakeAuditService.verifySent(EmailAuditEvent(psa, pstr,eventType, Delivered)) mustBe true
            fakeAuditService.verifySent(EmailAuditEvent(psa,pstr, eventType, Opened)) mustBe false
          }
        }
      }
    }
  }

  "respond with BAD_REQUEST when not given EmailEvents" in {

    running(_.overrides(
      bind[AuditService].to(fakeAuditService)
    )) { app =>

      fakeAuditService.reset()

      val encryptedPsa = app.injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(psa.id)).value
      val encryptedPstr = app.injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(pstr)).value
      val controller = app.injector.instanceOf[EmailResponseController]

      val result = controller.retrieveStatus(JourneyType.SCHEME_MIG, encryptedPsa, encryptedPstr)(fakeRequest.withBody(validJson))

      status(result) mustBe BAD_REQUEST
      fakeAuditService.verifyNothingSent mustBe true

    }

  }

  "respond with FORBIDDEN" when {
    "URL contains an id does not match PSAID pattern" in {

      running(_.overrides(
        bind[AuditService].to(fakeAuditService)
      )) { app =>

        fakeAuditService.reset()

        val psa = app.injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText("psa")).value
        val pstr = app.injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText("pstr")).value

        val controller = app.injector.instanceOf[EmailResponseController]

        val result = controller.retrieveStatus(JourneyType.RACDAC_IND_MIG,psa, pstr)(fakeRequest.withBody(Json.toJson(emailEvents)))

        status(result) mustBe FORBIDDEN
        contentAsString(result) mustBe "Malformed PSAID"
        fakeAuditService.verifyNothingSent mustBe true

      }
    }
  }

}

object EmailResponseControllerSpec {

  val psa = PsaId("A7654321")
  val pstr = "A0000030"
  val emailEvents = EmailEvents(Seq(EmailEvent(Sent, DateTime.now()), EmailEvent(Delivered, DateTime.now()), EmailEvent(Opened, DateTime.now())))

  val fakeAuditService = new StubSuccessfulAuditService()

  val validJson = Json.obj("name" -> "value")

}
