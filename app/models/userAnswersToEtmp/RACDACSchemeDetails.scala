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

package models.userAnswersToEtmp

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsPath, Json, Reads}

case class RACDACSchemeDetails(
                                racdacName: String,
                                contractOrPolicyNumber: String,
                                registrationStartDate: String
                              )

object RACDACSchemeDetails {
  val reads: Reads[RACDACSchemeDetails] =
    (
      (JsPath \ "schemeName").read[String] and
        ( (JsPath \ "contractOrPolicyNumber").read[String]
          orElse((JsPath \ "policyNumber").read[String])
          orElse((JsPath \ "policyNo").read[String])) and
        (JsPath \ "schemeOpenDate").read[String]
      ) (
      (name, contractOrPolicyNumber, registrationStartDate) => RACDACSchemeDetails(name, contractOrPolicyNumber, registrationStartDate)
    )

  implicit val formats: Format[RACDACSchemeDetails] = Json.format[RACDACSchemeDetails]
}
