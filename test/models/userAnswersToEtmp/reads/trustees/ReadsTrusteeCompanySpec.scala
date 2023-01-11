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

package models.userAnswersToEtmp.reads.trustees

import models.userAnswersToEtmp.Address
import models.userAnswersToEtmp.reads.CommonGenerator.trusteeCompanyGenerator
import models.userAnswersToEtmp.trustee.CompanyTrustee
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Gen, Shrink}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsArray, JsBoolean, JsString, Json}
import utils.PensionSchemeGenerators
import utils.UtrHelper.stripUtr

class ReadsTrusteeCompanySpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  "A Json payload containing trustee company" should {

    "read company name correctly" in {
      forAll(trusteeCompanyGenerator()) { json =>
        val model = json.as[CompanyTrustee](CompanyTrustee.readsTrusteeCompany)

        model.organizationName mustBe (json \ "companyDetails" \ "companyName").as[String]
      }
    }

    "must read vat when it is present" in {
      forAll(trusteeCompanyGenerator(), arbitrary[String]) {
        (json, vat) =>
          val newJson = json + ("vat" -> Json.obj("value" -> vat))
          val model = newJson.as[CompanyTrustee](CompanyTrustee.readsTrusteeCompany)
          model.vatRegistrationNumber.value mustBe (newJson \ "vat" \ "value").as[String]
      }
    }

    "must read paye when it is present" in {
      forAll(trusteeCompanyGenerator(), arbitrary[String]) {
        (json, paye) =>
          val newJson = json + ("paye" -> Json.obj("value" -> paye))
          val model = newJson.as[CompanyTrustee](CompanyTrustee.readsTrusteeCompany)
          model.payeReference.value mustBe (newJson \ "paye" \ "value").as[String]
      }
    }

    "must read utr when it is present" in {
      forAll(trusteeCompanyGenerator(), utrGeneratorFromUser) {
        (json, utr) =>
          val newJson = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[CompanyTrustee](CompanyTrustee.readsTrusteeCompany)
          model.utr mustBe stripUtr(Some(utr))
      }
    }

    "must read no utr reason when it is present" in {
      forAll(trusteeCompanyGenerator(), arbitrary[String]) {
        (json, noUtrReason) =>
          val newJson = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[CompanyTrustee](CompanyTrustee.readsTrusteeCompany)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }

    "must read crn when it is present" in {
      forAll(trusteeCompanyGenerator(), arbitrary[String]) {
        (json, vat) =>
          val newJson = json + ("companyNumber" -> Json.obj("value" -> vat))
          val model = newJson.as[CompanyTrustee](CompanyTrustee.readsTrusteeCompany)
          model.crnNumber.value mustBe (newJson \ "companyNumber" \ "value").as[String]
      }
    }

    "must read no crn reason when it is present" in {
      forAll(trusteeCompanyGenerator(), arbitrary[String]) {
        (json, noUtrReason) =>
          val newJson = json + ("noCompanyNumberReason" -> JsString(noUtrReason))
          val model = newJson.as[CompanyTrustee](CompanyTrustee.readsTrusteeCompany)
          model.noCrnReason.value mustBe (newJson \ "noCompanyNumberReason").as[String]
      }
    }

    "must read previous address when address years is under a year" in {
      forAll(trusteeCompanyGenerator(), false) {
        (json, addressYears) =>
          val newJson = json + ("addressYears" -> JsBoolean(addressYears))
          val model = newJson.as[CompanyTrustee](CompanyTrustee.readsTrusteeCompany)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "previousAddress").as[Address]
      }
    }

    "must not read previous address when address years is not under a year" in {
      forAll(trusteeCompanyGenerator(),true) {
        (json, addressYears) =>
        val newJson = json + ("addressYears" -> JsBoolean(addressYears))
        val model = newJson.as[CompanyTrustee](CompanyTrustee.readsTrusteeCompany)
          model.previousAddressDetails mustBe None
      }
    }

    "read company address correctly" in {
      forAll(trusteeCompanyGenerator()) { json =>
        val model = json.as[CompanyTrustee](CompanyTrustee.readsTrusteeCompany)

        model.correspondenceAddressDetails.addressDetails mustBe
          (json \ "address").as[Address]
      }
    }

    "read company contact details correctly" in {
      forAll(trusteeCompanyGenerator()) { json =>
        val model = json.as[CompanyTrustee](CompanyTrustee.readsTrusteeCompany)

        model.correspondenceContactDetails.contactDetails.email mustBe
          (json \ "email").as[String]

        model.correspondenceContactDetails.contactDetails.telephone mustBe
          (json  \ "phone").as[String]
      }
    }
  }

  def arbitraryString: Gen[String] = Gen.alphaStr suchThat (_.nonEmpty)

  val companiesGen: Gen[JsArray] =
    for {
      company1 <- trusteeCompanyGenerator()
      company2 <- trusteeCompanyGenerator(true)
      company3 <- trusteeCompanyGenerator()
    } yield Json.arr(company1, company2, company3)
}
