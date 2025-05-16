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
import play.api.libs.json._

case class Company(name: String, vatNumber: Option[String], payeNumber: Option[String], utr: Option[String],
                   noUtrReason: Option[String], crn: Option[String], noCrnReason: Option[String], address: Address,
                   contactDetails: ContactDetails, tradingTime: Option[Boolean], previousAddress: Option[Address], addressYears: Boolean)

object Company {

  def companyReads: Reads[Company] = (
    (JsPath \ "companyDetails" \ "companyName").read[String] and
      (JsPath \ "vat").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "paye").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noUtrReason").readNullable[String] and
      (JsPath \ "companyNumber").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noCompanyNumberReason").readNullable[String] and
      (JsPath \ "address").read[Address] and
      ContactDetails.readsContactDetails and
      (JsPath \ "tradingTime").readNullable[Boolean] and
      (JsPath \ "previousAddress").readNullable[Address] and
      (JsPath \ "addressYears").read[Boolean]
    ) (Company.apply)
}
