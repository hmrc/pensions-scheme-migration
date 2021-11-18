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

package models.userAnswersToEtmp.reads.establishers

import models.userAnswersToEtmp.Address
import models.userAnswersToEtmp.establisher.CompanyEstablisher
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Shrink
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues
import play.api.libs.json._
import models.userAnswersToEtmp.reads.CommonGenerator.establisherCompanyGenerator
import utils.PensionSchemeGenerators
import utils.UtrHelper.stripUtr

class ReadsEstablisherCompanySpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  "A Json payload containing establisher company" should {

    "read company name correctly" in {
      forAll(establisherCompanyGenerator()) { json =>
        val model = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)

        model.organizationName mustBe (json \ "companyDetails" \ "companyName").as[String]
      }
    }

    "must read vat when it is present" in {
      forAll(establisherCompanyGenerator(), arbitrary[String]) {
        (json, vat) =>
          val newJson = json + ("vat" -> Json.obj("value" -> vat))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.vatRegistrationNumber.value mustBe (newJson \ "vat" \ "value").as[String]
      }
    }

    "must read paye when it is present" in {
      forAll(establisherCompanyGenerator(), arbitrary[String]) {
        (json, paye) =>
          val newJson = json + ("paye" -> Json.obj("value" -> paye))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.payeReference.value mustBe (newJson \ "paye" \ "value").as[String]
      }
    }

    "must read utr when it is present" in {
      forAll(establisherCompanyGenerator(), utrGeneratorFromUser) {
        (json, utr) =>
          val newJson = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.utr mustBe stripUtr(Some(utr))
      }
    }

    "must read no utr reason when it is present" in {
      forAll(establisherCompanyGenerator(), arbitrary[String]) {
        (json, noUtrReason) =>
          val newJson = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }

    "must read crn when it is present" in {
      forAll(establisherCompanyGenerator(), arbitrary[String]) {
        (json, vat) =>
          val newJson = json + ("companyNumber" -> Json.obj("value" -> vat))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.crnNumber.value mustBe (newJson \ "companyNumber" \ "value").as[String]
      }
    }

    "must read no crn reason when it is present" in {
      forAll(establisherCompanyGenerator(), arbitrary[String]) {
        (json, noUtrReason) =>
          val newJson = json + ("noCompanyNumberReason" -> JsString(noUtrReason))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.noCrnReason.value mustBe (newJson \ "noCompanyNumberReason").as[String]
      }
    }

    "read company address correctly" in {
      forAll(establisherCompanyGenerator()) { json =>
        val model = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)

        model.correspondenceAddressDetails.addressDetails mustBe
          (json \ "address").as[Address]
      }
    }

    "must read previous address when address years is under a year and trading time is true" in {
      forAll(establisherCompanyGenerator(), false, true) {
        (json, addressYears, hasBeenTrading) =>
          val newJson = json + ("addressYears" -> JsBoolean(addressYears)) + ("tradingTime" -> JsBoolean(hasBeenTrading))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "previousAddress").as[Address]
      }
    }

    "must not read previous address when address years is not under a year" in {
      forAll(establisherCompanyGenerator(),true) {
        (json, addressYears) =>
          val newJson = json + ("addressYears" -> JsBoolean(addressYears))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.previousAddressDetails mustBe None
      }
    }

    "must not read previous address when address years is under a year but trading time is false" in {
      forAll(establisherCompanyGenerator(),false, false) {
        (json,addressYears, hasBeenTrading) =>
          val newJson = json + ("addressYears" -> JsBoolean(addressYears)) + ("tradingTime" -> JsBoolean(hasBeenTrading))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.previousAddressDetails mustBe None
      }
    }

    "read company contact details correctly" in {
      forAll(establisherCompanyGenerator()) { json =>
        val model = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)

        model.correspondenceContactDetails.contactDetails.email mustBe
          (json  \ "email").as[String]

        model.correspondenceContactDetails.contactDetails.telephone mustBe
          (json \ "phone").as[String]
      }
    }

    "read other directors flag correctly" in {
      forAll(establisherCompanyGenerator()) { json =>
        val model = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)

        model.haveMoreThanTenDirectorOrPartner mustBe (json \ "otherDirectors").as[Boolean]
      }
    }

    "read directors correctly" in {
      forAll(establisherCompanyGenerator()) { json =>
        val model = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)

        model.directorDetails.head.personalDetails.firstName mustBe
          (json \ "director" \ 0 \ "directorDetails" \ "firstName").as[String]

        model.directorDetails(1).personalDetails.firstName mustBe
          (json \ "director" \ 2 \ "directorDetails" \ "firstName").as[String]
      }
    }
  }
}
