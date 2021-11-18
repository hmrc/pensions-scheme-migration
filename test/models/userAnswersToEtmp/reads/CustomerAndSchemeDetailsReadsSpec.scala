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

package models.userAnswersToEtmp.reads

import models.userAnswersToEtmp.{UkAddress, CustomerAndSchemeDetails}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json._

class CustomerAndSchemeDetailsReadsSpec extends AnyWordSpec with Matchers {

  import CustomerAndSchemeDetailsReadsSpec._

  //scalastyle:off method.length
  private def readsTests(): Unit = {
    "correctly parse to the corresponding CustomerAndSchemeDetailsReads involving scheme name or type" when {
      "is Group Life/Death" in {

        val result = (dataJson ++ Json.obj(
          "schemeName" -> "test scheme name",
          "schemeType" -> Json.obj(
            "name" -> "group"
          ))).as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)

        result.schemeStructure mustBe customerDetails.copy(schemeStructure = Some("A group life/death in service scheme")).schemeStructure
      }

      "is Body Corporate" in {
        val result = (dataJson ++ Json.obj(
          "schemeName" -> "test scheme name",
          "schemeType" -> Json.obj(
            "name" -> "corp"
          ))).as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)

        result.schemeStructure mustBe customerDetails.copy(schemeStructure = Some("A body corporate")).schemeStructure
      }

      "is Other with other Scheme structure" in {
        val result = (dataJson ++ Json.obj(
          "schemeName" -> "test scheme name",
          "schemeType" -> Json.obj(
            "name" -> "other",
            "schemeTypeDetails" -> "other details"
          ))).as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)

        result.schemeStructure mustBe customerDetails.copy(schemeStructure = Some("Other")).schemeStructure
      }

      "is Master trust" in {
        val result = (dataJson ++ Json.obj(
          "schemeName" -> "test scheme name",
          "schemeType" -> Json.obj(
            "name" -> "master"
          ))).as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)

        result.isSchemeMasterTrust mustBe true
      }

      "we have Master Trust" when {
        "scheme structure is None" in {
          val result = (dataJson ++ Json.obj(
            "schemeName" -> "test scheme name",
            "schemeType" -> Json.obj(
              "name" -> "master"
            ))).as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)

          result.schemeStructure mustBe None
        }
      }

      "we have insurance policy name and number" that {
        "is with valid insurance company name but no policy number" in {
          val json = dataJson ++ Json.obj("insuranceCompanyName" -> customerDetails.insuranceCompanyName)
          val result = json.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
          result.insuranceCompanyName mustBe customerDetails.insuranceCompanyName
          result.policyNumber mustBe None
        }

        "is with valid policy number but no company name" in {
          val json = dataJson ++ Json.obj("insurancePolicyNumber" -> customerDetails.policyNumber)
          val result = json.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
          result.policyNumber mustBe customerDetails.policyNumber
          result.insuranceCompanyName mustBe None
        }

        "is with valid policy number and insurance company name" in {
          val json = dataJson ++ Json.obj("insuranceCompanyName" -> customerDetails.insuranceCompanyName,
            "insurancePolicyNumber" -> customerDetails.policyNumber)
          val result = json.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
          result.insuranceCompanyName mustBe customerDetails.insuranceCompanyName
          result.policyNumber mustBe customerDetails.policyNumber
        }
      }
    }

    readsTestsCommon(dataJson, CustomerAndSchemeDetails.apiReads)
  }

  //scalastyle:off method.length
  private def readsTestsCommon(jsObject: JsObject, readsCustomerAndSchemeDetails: Reads[CustomerAndSchemeDetails]): Unit = {
    "correctly parse to the corresponding CustomerAndSchemeDetailsReads" when {

      "we have a valid scheme name" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.schemeName mustBe customerDetails.schemeName
      }

      "we have a scheme type which is not master trust" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.isSchemeMasterTrust mustBe customerDetails.isSchemeMasterTrust
      }

      "we have scheme structure" that {
        "is Single Trust with no other scheme structure" in {
          val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
          result.schemeStructure mustBe customerDetails.schemeStructure
          result.otherSchemeStructure mustBe None
        }

        "it is not a Master trust" in {
          val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)

          result.isSchemeMasterTrust mustBe false
        }
      }

      "we have a valid more than ten trustees flag" in {
        val result = (jsObject + ("otherTrustees" -> JsBoolean(true))).as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.haveMoreThanTenTrustee mustBe customerDetails.haveMoreThanTenTrustee
      }

      "we don't have more than ten trustees flag" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.haveMoreThanTenTrustee mustBe None
      }

      "we have a valid membership" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.currentSchemeMembers mustBe customerDetails.currentSchemeMembers
      }

      "we have a valid future membership" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.futureSchemeMembers mustBe customerDetails.futureSchemeMembers
      }

      "we have a valid investment regulated flag" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.isRegulatedSchemeInvestment mustBe customerDetails.isRegulatedSchemeInvestment
      }

      "we have a valid occupational pension scheme flag" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.isOccupationalPensionScheme mustBe customerDetails.isOccupationalPensionScheme
      }

      "we have a valid secured benefits flag" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.areBenefitsSecuredContractInsuranceCompany mustBe customerDetails.areBenefitsSecuredContractInsuranceCompany
      }

      "we have a valid scheme benefits" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.doesSchemeProvideBenefits mustBe customerDetails.doesSchemeProvideBenefits
      }

      "we have a valid scheme established country" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.schemeEstablishedCountry mustBe customerDetails.schemeEstablishedCountry
      }

      "we have a invalid bank account as false" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.haveInvalidBank mustBe customerDetails.haveInvalidBank
      }

      "we don't have benefits insurer" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.insuranceCompanyName mustBe customerDetails.copy(insuranceCompanyName = None, policyNumber = None).insuranceCompanyName
      }

      "we have benefits insurer address" in {
        val result = (jsObject + ("insurerAddress" -> Json.obj(
          "addressLine1" -> "ADDRESS LINE 1",
          "addressLine2" -> "ADDRESS LINE 2",
          "addressLine3" -> "ADDRESS LINE 3",
          "addressLine4" -> "ADDRESS LINE 4",
          "postcode" -> "ZZ1 1ZZ",
          "country" -> "GB"))).as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.insuranceCompanyAddress mustBe customerDetails.insuranceCompanyAddress
      }

      "we don't have benefits insurer address" in {
        val result = jsObject.as[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
        result.insuranceCompanyAddress mustBe None
      }
    }
  }

  "Json Payload containing Customer and scheme details" must {
    behave like readsTests
  }

}

object CustomerAndSchemeDetailsReadsSpec {
  val customerDetails: CustomerAndSchemeDetails = CustomerAndSchemeDetails("test scheme name", isSchemeMasterTrust = false, schemeStructure = Some("A single trust under which all" +
    " of the assets are held for the benefit of all members of the scheme"), otherSchemeStructure = Some("other details"),
    haveMoreThanTenTrustee = Some(true), currentSchemeMembers = "2 to 11", futureSchemeMembers = "0", isRegulatedSchemeInvestment = true, isOccupationalPensionScheme = true,
    areBenefitsSecuredContractInsuranceCompany = true, doesSchemeProvideBenefits = "Defined Benefits only", tcmpBenefitType = None,
    schemeEstablishedCountry = "GB", haveInvalidBank = false, insuranceCompanyName = Some("my insurance company"), policyNumber = Some("111"),
    insuranceCompanyAddress = Some(UkAddress("ADDRESS LINE 1", Some("ADDRESS LINE 2"), Some("ADDRESS LINE 3"), Some("ADDRESS LINE 4"), "GB", "ZZ1 1ZZ")))


  val dataJson: JsObject =
    Json.obj(
      "schemeName" -> "test scheme name",
      "schemeType" -> Json.obj(
        "name" -> "single"
      ),
      "currentMembers" -> "opt3",
      "futureMembers" -> "opt1",
      "investmentRegulated" -> true,
      "occupationalPensionScheme" -> true,
      "securedBenefits" -> true,
      "benefits" -> "definedBenefitsOnly",
      "schemeEstablishedCountry" -> "GB"
    )

}


