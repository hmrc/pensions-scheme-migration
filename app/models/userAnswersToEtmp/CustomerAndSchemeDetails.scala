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

package models.userAnswersToEtmp

import models.enumeration.{BenefitsProvisionType, BenefitsType, SchemeMembers, SchemeType}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class CustomerAndSchemeDetails(schemeName: String, isSchemeMasterTrust: Boolean, schemeStructure: Option[String],
                                    otherSchemeStructure: Option[String] = None, haveMoreThanTenTrustee: Option[Boolean] = None, currentSchemeMembers: String,
                                    futureSchemeMembers: String, isRegulatedSchemeInvestment: Boolean, isOccupationalPensionScheme: Boolean,
                                    areBenefitsSecuredContractInsuranceCompany: Boolean, doesSchemeProvideBenefits: String, tcmpBenefitType: Option[String],
                                    schemeEstablishedCountry: String, haveInvalidBank: Boolean, insuranceCompanyName: Option[String] = None,
                                    policyNumber: Option[String] = None, insuranceCompanyAddress: Option[Address] = None)

object CustomerAndSchemeDetails {
  implicit val formats: Format[CustomerAndSchemeDetails] = Json.format[CustomerAndSchemeDetails]

  private val schemeTypeReads: Reads[(String, Option[String])] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "schemeTypeDetails").readNullable[String]
    ) ((name, schemeDetails) => (name, schemeDetails))

  def apiReads: Reads[CustomerAndSchemeDetails] = (
    (JsPath \ "schemeName").read[String] and
      (JsPath \ "schemeType").read[(String, Option[String])](schemeTypeReads) and
      (JsPath \ "otherTrustees").readNullable[Boolean] and
      (JsPath \ "currentMembers").read[String] and
      (JsPath \ "futureMembers").read[String] and
      (JsPath \ "investmentRegulated").read[Boolean] and
      (JsPath \ "occupationalPensionScheme").read[Boolean] and
      (JsPath \ "securedBenefits").read[Boolean] and
      (JsPath \ "schemeEstablishedCountry").read[String] and
      (JsPath \ "insuranceCompanyName").readNullable[String] and
      (JsPath \ "insurancePolicyNumber").readNullable[String] and
      (JsPath \ "insurerAddress").readNullable[Address] and
      benefitsReads
    ) (
    (name, schemeType, moreThanTenTrustees, membership, membershipFuture, investmentRegulated, occupationalPension, securedBenefits, country,
     insuranceCompanyName, insurancePolicyNumber, insurerAddress, benefits) => {
      val (schemeName, otherScheme) = schemeType
      val isMasterTrust = (schemeName == "master")

      val schemeTypeName = if (isMasterTrust) None else Some(SchemeType.valueWithName(schemeName))

      CustomerAndSchemeDetails(
        schemeName = name,
        isSchemeMasterTrust = isMasterTrust,
        schemeStructure = schemeTypeName,
        otherSchemeStructure = otherScheme,
        haveMoreThanTenTrustee = moreThanTenTrustees,
        currentSchemeMembers = SchemeMembers.valueWithName(membership),
        futureSchemeMembers = SchemeMembers.valueWithName(membershipFuture),
        isRegulatedSchemeInvestment = investmentRegulated,
        isOccupationalPensionScheme = occupationalPension,
        areBenefitsSecuredContractInsuranceCompany = securedBenefits,
        doesSchemeProvideBenefits = benefits._1,
        tcmpBenefitType = benefits._2,
        schemeEstablishedCountry = country,
        haveInvalidBank = false,
        insuranceCompanyName = insuranceCompanyName,
        policyNumber = insurancePolicyNumber,
        insuranceCompanyAddress = insurerAddress
      )
    }
  )

  private def benefitsReads: Reads[(String, Option[String])] =
    (JsPath \ "benefits").read[String] flatMap {
      case benefits if !benefits.equalsIgnoreCase("definedBenefitsOnly") =>
        moneyPurchaseBenefits(benefits)
      case _ =>
        (JsPath \ "benefits").read[String].map(benefits =>
          (BenefitsProvisionType.valueWithName(benefits), None: Option[String]))
    }

  private def moneyPurchaseBenefits(benefits: String): Reads[(String, Option[String])] =
    (JsPath \ "moneyPurchaseBenefits").read[String].map {
      moneyPurchaseBenefits =>
        (BenefitsProvisionType.valueWithName(benefits), Some(BenefitsType.valueWithName(moneyPurchaseBenefits)))
    }
}
