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

package models.userAnswersToEtmp.reads.establishers

import models.userAnswersToEtmp.Address
import models.userAnswersToEtmp.reads.CommonGenerator.establisherPartnershipGenerator
import models.userAnswersToEtmp.establisher.Partnership
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Shrink
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json._
import utils.PensionSchemeGenerators
import utils.UtrHelper.stripUtr

class ReadsEstablisherPartnershipSpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators {
  private implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  "A Json payload containing trustee partnership" should {
    "have partnership name read correctly" in {
      forAll(establisherPartnershipGenerator()) { json =>
        val transformedEstablisher = json.as[Partnership](Partnership.readsEstablisherPartnership)
        transformedEstablisher.organizationName mustBe (json \ "partnershipDetails" \ "partnershipName").as[String]
      }
    }

    "must read vat when it is present" in {
      forAll(establisherPartnershipGenerator(), arbitrary[String]) {
        (json, vat) =>
          val newJson = json + ("vat" -> Json.obj("value" -> vat))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.vatRegistrationNumber.value mustBe (newJson \ "vat" \ "value").as[String]
      }
    }

    "must read paye when it is present" in {
      forAll(establisherPartnershipGenerator(), arbitrary[String]) {
        (json, paye) =>
          val newJson = json + ("paye" -> Json.obj("value" -> paye))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.payeReference.value mustBe (newJson \ "paye" \ "value").as[String]
      }
    }

    "must read utr when it is present" in {
      forAll(establisherPartnershipGenerator(), utrGeneratorFromUser) {
        (json, utr) =>
          val newJson = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.utr mustBe stripUtr(Some(utr))
      }
    }

    "must read no utr reason when it is present" in {
      forAll(establisherPartnershipGenerator(), arbitrary[String]) {
        (json, noUtrReason) =>
          val newJson = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }

    "read partnership address correctly" in {
      forAll(establisherPartnershipGenerator()) { json =>
        val model = json.as[Partnership](Partnership.readsEstablisherPartnership)

        model.correspondenceAddressDetails.addressDetails mustBe
          (json \ "address").as[Address]
      }
    }

    "must read previous address when address years is under a year and trading time is true" in {
      forAll(establisherPartnershipGenerator(), false, true) {
        (json, addressYears, hasBeenTrading) =>
          val newJson = json + ("addressYears" -> JsBoolean(addressYears)) + ("tradingTime" -> JsBoolean(hasBeenTrading))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "previousAddress").as[Address]
      }
    }

    "must not read previous address when address years is not under a year" in {
      forAll(establisherPartnershipGenerator(),true) {
        ( json,addressYears) =>
          val newJson = json + ("addressYears" -> JsBoolean(addressYears))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.previousAddressDetails mustBe None
      }
    }

    "must not read previous address when address years is under a year but trading time is false" in {
      forAll(establisherPartnershipGenerator(), false) {
        (json, hasBeenTrading) =>
          val newJson = json + ("tradingTime" -> JsBoolean(hasBeenTrading))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.previousAddressDetails mustBe None
      }
    }

    "read partnership contact details correctly" in {
      forAll(establisherPartnershipGenerator()) { json =>
        val model = json.as[Partnership](Partnership.readsEstablisherPartnership)

        model.correspondenceContactDetails.contactDetails.email mustBe
          (json \ "partnershipEmail").as[String]

        model.correspondenceContactDetails.contactDetails.telephone mustBe
          (json \ "partnershipPhone").as[String]
      }
    }

    "read other partners flag correctly" in {
      forAll(establisherPartnershipGenerator()) { json =>
        val model = json.as[Partnership](Partnership.readsEstablisherPartnership)

        model.haveMoreThanTenDirectorOrPartner mustBe (json \ "otherPartners").as[Boolean]
      }
    }

    "read partners correctly" in {
      forAll(establisherPartnershipGenerator()) { json =>
        val model = json.as[Partnership](Partnership.readsEstablisherPartnership)

        model.partnerDetails.head.personalDetails.firstName mustBe
          (json \ "partner" \ 0 \ "partnerDetails" \ "firstName").as[String]

        model.partnerDetails(1).personalDetails.firstName mustBe
          (json \ "partner" \ 1 \ "partnerDetails" \ "firstName").as[String]
      }
    }
  }
}
