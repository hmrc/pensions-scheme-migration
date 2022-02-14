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

package models.userAnswersToEtmp

import models.userAnswersToEtmp.ReadsHelper.previousAddressDetails
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.UtrHelper.stripUtr

case class Individual(
                       personalDetails: PersonalDetails,
                       referenceOrNino: Option[String] = None,
                       noNinoReason: Option[String] = None,
                       utr: Option[String] = None,
                       noUtrReason: Option[String] = None,
                       correspondenceAddressDetails: CorrespondenceAddressDetails,
                       correspondenceContactDetails: CorrespondenceContactDetails,
                       previousAddressDetails: Option[PreviousAddressDetails] = None
                     )

object Individual {
  implicit val formats: Format[Individual] = Json.format[Individual]

  val readsCompanyDirector: Reads[Individual] = (
    PersonalDetails.readsPersonDetails(userAnswersBase = "directorDetails") and
      (JsPath \ "address").read[Address] and
      (JsPath \ "directorContactDetails").read[ContactDetails](ContactDetails.readsContactDetails) and
      (JsPath \ "nino").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noNinoReason").readNullable[String] and
      (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noUtrReason").readNullable[String] and
      (JsPath \ "addressYears").read[Boolean] and
      (JsPath \ "previousAddress").readNullable[Address]
    ) ((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    Individual(
      personalDetails = personalDetails,
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = stripUtr(utr),
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )

  val readsPartner: Reads[Individual] = (
    PersonalDetails.readsPersonDetails(userAnswersBase = "partnerDetails") and
      (JsPath \ "address").read[Address] and
      (JsPath \ "partnerContactDetails").read[ContactDetails](ContactDetails.readsContactDetails) and
      (JsPath \ "nino").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noNinoReason").readNullable[String] and
      (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noUtrReason").readNullable[String] and
      (JsPath \ "addressYears").read[Boolean] and
      (JsPath \ "previousAddress").readNullable[Address]
    ) ((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    Individual(
      personalDetails = personalDetails,
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = stripUtr(utr),
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )

  val readsEstablisherIndividual: Reads[Individual] = (
    PersonalDetails.readsPersonDetails("establisherDetails") and
      (JsPath \ "address").read[Address] and
       ContactDetails.readsContactDetails and
      (JsPath \ "nino").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noNinoReason").readNullable[String] and
      (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noUtrReason").readNullable[String] and
      (JsPath \ "addressYears").read[Boolean] and
      (JsPath \ "previousAddress").readNullable[Address]
    ) ((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    Individual(
      personalDetails = personalDetails,
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = stripUtr(utr),
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = ReadsHelper.previousAddressDetails(addressYears, previousAddress)
    )
  )

  val readsTrusteeIndividual: Reads[Individual] = (
    PersonalDetails.readsPersonDetails(userAnswersBase = "trusteeDetails") and
      (JsPath \ "address").read[Address] and
      ContactDetails.readsContactDetails and
      (JsPath \ "nino").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noNinoReason").readNullable[String] and
      (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noUtrReason").readNullable[String] and
      (JsPath \ "addressYears").read[Boolean] and
      (JsPath \ "previousAddress").readNullable[Address]
    ) ((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    Individual(
      personalDetails = personalDetails,
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = stripUtr(utr),
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )
}

