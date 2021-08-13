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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class Items(pstr: String, declarationDate: String, racDac: Boolean, schemeName: String, schemeOpenDate: String,
                 policyNo: Option[String])

object Items {
  implicit val ifReads: Reads[Items] = (
    (JsPath \ "pstr").read[String] and
      (JsPath \ "declarationDate").read[String] and
      (JsPath \ "racDac").read[Boolean] and
      (JsPath \ "schemeName").read[String] and
      (JsPath \ "schemeOpenDate").read[String] and
      (JsPath \ "policyNo").readNullable[String]
    ) (
    (pstr, declarationDate, racDac, schemeName, schemeOpenDate, policyNo) => {
      val policy = if (racDac) policyNo else None
      Items(pstr, declarationDate, racDac, schemeName, schemeOpenDate, policy)
    }
  )
  implicit val ifWrites: Writes[Items] = Json.writes[Items]
}

case class ListOfLegacySchemes(totalResults: Int,
                               items: Option[List[Items]] = None)

object ListOfLegacySchemes {
  implicit val ifWrites: Writes[ListOfLegacySchemes] = Json.writes[ListOfLegacySchemes]

  implicit val ifReads: Reads[ListOfLegacySchemes] = (
    (JsPath \ "totalResults").read[Int] and
      (JsPath \ "items").readNullable[List[Items]](Reads.list(Items.ifReads))
    ) (
    (totalResults, items) =>
      ListOfLegacySchemes(totalResults, items)
  )
}
