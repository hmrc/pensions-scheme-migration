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

package audit

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class SchemeMigrationAuditEventSpec extends AnyWordSpec with Matchers {

  private val psaId = "A2500001"
  private val pstr = "A0000030"
  private val status = 200
  private val request=Json.obj("name" -> "abc")
  private val payload = Json.toJson(Json.obj("name" -> "abc"))


  private val event = SchemeMigrationAuditEvent(psaId, pstr, status,request, Some(payload))

  private val expectedDetails = Map(
    "psaId" -> psaId,
    "pstr" -> pstr,
    "status" -> status.toString,
    "request" -> request.toString,
    "response" -> payload.toString
  )

  "calling SchemeMigrationAuditEvent" must {

    "returns correct event object" in {

      event.auditType mustBe "SchemeMigrationAudit"

      event.details mustBe expectedDetails
    }
  }
}
