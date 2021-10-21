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

import play.api.libs.json.{JsValue, Json}

case class SchemeDetailsAuditEvent(
                                    psaId: String,
                                    pstr: String,
                                    status: Int,
                                    payload: Option[JsValue]
                                  ) extends AuditEvent {

  override def auditType: String = "GetSchemeDetails"

  override def details: Map[String, String] = Map(
    "psaId"     ->    psaId,
    "pstr"      ->    pstr,
    "status"    ->    status.toString,
    "payload"   ->    payload.fold("")(Json.stringify)
  )
}
