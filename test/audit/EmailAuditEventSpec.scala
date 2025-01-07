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

package audit

import models.Sent
import models.enumeration.JourneyType
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.domain.PsaId

class EmailAuditEventSpec extends AsyncFlatSpec with Matchers {

  "EmailAuditEvent" should "output the correct map of data" in {

    val event = EmailAuditEvent(
      psaId = PsaId("A2500001"),
      pstrId = "pstr-test",
      journeyType = JourneyType.SCHEME_MIG,
      event = Sent
    )

    val expected = Map(
      "psaId" -> "A2500001",
      "event" -> Sent.toString,
      "pstr" -> "pstr-test"
    )

    event.auditType shouldBe "SchemeMigrationEmailStatusEvent"
    event.details shouldBe expected
  }

  "EmailAuditEventPsa" should "output the correct map of data" in {

    val event = EmailAuditEventPsa(
      psaId = PsaId("A2500001"),
      journeyType = JourneyType.RACDAC_IND_MIG,
      event = Sent
    )

    val expected = Map(
      "psaId" -> "A2500001",
      "event" -> Sent.toString
    )

    event.auditType shouldBe "RetirementOrDeferredAnnuityContractMigrationEmailStatusEvent"
    event.details shouldBe expected
  }

  "EmailRequestAuditEvent" should "output the correct map of data" in {

    val event = EmailRequestAuditEvent(
      psaId = "A2500001",
      journeyType = JourneyType.RACDAC_BULK_MIG,
      emailAddress = "test@test.com",
      pstrId = "pstr-test"
    )

    val expected = Map(
      "psaId" -> "A2500001",
      "emailAddress" -> "test@test.com",
      "pstr" -> "pstr-test"
    )

    event.auditType shouldBe "RetirementOrDeferredAnnuityContractBulkMigrationEmailSentEvent"
    event.details shouldBe expected
  }
}
