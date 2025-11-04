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

import models.userAnswersToEtmp.*
import models.userAnswersToEtmp.reads.CommonGenerator.trusteePartnershipGenerator
import models.userAnswersToEtmp.trustee.PartnershipTrustee
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Gen, Shrink}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsBoolean, JsString, Json}
import utils.PensionSchemeGenerators
import utils.UtrHelper.stripUtr

class ReadsTrusteePartnershipSpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators {
  private implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny
  private def arbitraryString: Gen[String] =  Gen.alphaStr suchThat (_.nonEmpty)

  "A Json payload containing trustee partnership" must {

    "have partnership name read correctly" in {
      forAll(trusteePartnershipGenerator()) { json =>
        val transformedTrustee = json.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
        transformedTrustee.organizationName mustBe (json \ "partnershipDetails" \ "partnershipName").as[String]
      }
    }

    "read utr when it is present" in {
      forAll(trusteePartnershipGenerator(), utrGeneratorFromUser) {
        (json, utr) =>
          val newJson = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.utr mustBe stripUtr(Some(utr))
      }
    }

    "read no utr reason when it is present" in {
      forAll(trusteePartnershipGenerator(), arbitraryString) {
        (json, noUtrReason) =>
          val newJson = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }

    "read vat when it is present" in {
      forAll(trusteePartnershipGenerator(), arbitraryString) {
        (json, vat) =>
          val newJson = json + ("vat" -> Json.obj("value" -> vat))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.vatRegistrationNumber.value mustBe (newJson \ "vat" \ "value").as[String]
      }
    }

    "read vat when it is present removing space characters" in {
      forAll(trusteePartnershipGenerator(), " 123  456 ") {
        (json, vat) =>
          val newJson = json + ("vat" -> Json.obj("value" -> vat))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.vatRegistrationNumber.value mustBe "123456"
      }
    }

    "read paye when it is present" in {
      forAll(trusteePartnershipGenerator(), arbitraryString) {
        (json, paye) =>
          val newJson = json + ("paye" -> Json.obj("value" -> paye))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.payeReference.value mustBe (newJson \ "paye" \ "value").as[String]
      }
    }

    "read paye when it is present removing space characters" in {
      forAll(trusteePartnershipGenerator(), " 123  456 ") {
        (json, paye) =>
          val newJson = json + ("paye" -> Json.obj("value" -> paye))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.payeReference.value mustBe "123456"
      }
    }

    "read previous address when address years is under a year" in {
      forAll(trusteePartnershipGenerator(), false) {
        (json, addressYears) =>
          val newJson = json + ("addressYears" -> JsBoolean(addressYears))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "previousAddress").as[Address]
      }
    }

    "read no previous address when address years is not under a year" in {
      forAll(trusteePartnershipGenerator(), true) {
        (json, addressYears) =>
          val newJson = json + ("addressYears" -> JsBoolean(addressYears))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.previousAddressDetails mustBe None
      }
    }

    "read company address correctly" in {
      forAll(trusteePartnershipGenerator()) { json =>
        val model = json.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)

        model.correspondenceAddressDetails.addressDetails mustBe (json \ "address").as[Address]
      }
    }

    "have partnership contact details read correctly" in {
      forAll(trusteePartnershipGenerator()) { json =>
        val model = json.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
        model.correspondenceContactDetails.contactDetails.email mustBe
          (json \ "partnershipEmail").as[String]

        model.correspondenceContactDetails.contactDetails.telephone mustBe
          (json \ "partnershipPhone").as[String]
      }
    }

    "have partnership contact details read correctly removing white space from email address" in {
      forAll(trusteePartnershipGenerator(email = Some(" an email with white space "))) { json =>
        val model = json.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
        model.correspondenceContactDetails.contactDetails.email mustBe
          "anemailwithwhitespace"

        model.correspondenceContactDetails.contactDetails.telephone mustBe
          (json \ "partnershipPhone").as[String]
      }
    }
  }
}
