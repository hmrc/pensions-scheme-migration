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

case class PensionSchemeDeclaration(box1: Option[Boolean] = None, box2: Option[Boolean] = None, box3: Option[Boolean] = None, box4: Option[Boolean] = None,
                                    box5: Option[Boolean] = None, box6: Boolean, box7: Boolean, box8: Boolean, box9: Option[Boolean] = None,
                                    box10: Option[Boolean] = None, box11: Option[Boolean] = None, pensionAdviserName: Option[String] = None,
                                    addressAndContactDetails: Option[AddressAndContactDetails] = None)

object PensionSchemeDeclaration {

  implicit val formats: Format[PensionSchemeDeclaration] = Json.format[PensionSchemeDeclaration]
  implicit val writes: OWrites[PensionSchemeDeclaration] = Json.writes[PensionSchemeDeclaration]

  val apiReads: Reads[PensionSchemeDeclaration] = (
      (JsPath \ "schemeType" \ "name").read[String] and
      (JsPath \ "declarationDormant").readNullable[String] and
      (JsPath \ "workingKnowledge").read[Boolean] and
      (JsPath \ "adviserName").readNullable[String] and
      (JsPath \ "adviserEmail").readNullable[String] and
      (JsPath \ "adviserPhone").readNullable[String] and
      (JsPath \ "adviserAddress").readNullable[Address]
    ) ((schemeTypeName, declarationDormant, declarationDuties, adviserName, adviserEmail, adviserPhone, adviserAddress) => {

    val declaration=true
    val basicDeclaration = PensionSchemeDeclaration(
      None,None, None, None, None,
      declaration,
      declaration,
      declaration,
      None, None, None, None)

    val isMasterTrust = (dec: PensionSchemeDeclaration) => {
      if (schemeTypeName == "master")
        dec.copy(box3 = Some(true))
      else
        dec
    }
    val decDuties = (dec: PensionSchemeDeclaration) => {

      if (declarationDuties) {
        dec.copy(box10 = Some(true))
      }
      else {
        dec.copy(
          box11 = Some(true),
          pensionAdviserName = adviserName,
          addressAndContactDetails = {
            (adviserEmail, adviserPhone, adviserAddress) match {
              case (Some(contactEmail), Some(contactPhone), Some(address)) =>
                Some(AddressAndContactDetails(
                  address,
                  ContactDetails(contactPhone, None, None, contactEmail)
                ))
              case _ => None
            }
          }
        )
      }

    }

    val completedDeclaration = isMasterTrust andThen decDuties
    completedDeclaration(basicDeclaration)
  })
}

case class AddressAndContactDetails(addressDetails: Address, contactDetails: ContactDetails)

object AddressAndContactDetails {
  implicit val formats: Format[AddressAndContactDetails] = Json.format[AddressAndContactDetails]
}
