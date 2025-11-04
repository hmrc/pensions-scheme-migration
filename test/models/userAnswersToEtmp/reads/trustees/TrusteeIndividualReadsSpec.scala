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

import models.userAnswersToEtmp.reads.CommonGenerator.trusteeIndividualGenerator
import models.userAnswersToEtmp.{Address, Individual}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Shrink
import org.scalatest.matchers.must.Matchers
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{JsBoolean, JsString, Json}
import utils.PensionSchemeGenerators
import utils.UtrHelper.stripUtr

class TrusteeIndividualReadsSpec extends AnyFreeSpec with Matchers with ScalaCheckDrivenPropertyChecks with OptionValues with PensionSchemeGenerators {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  "An trustee individual" - {

    "must read individual details" in {
      forAll(trusteeIndividualGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsTrusteeIndividual)
          model.personalDetails.firstName mustBe (json \ "trusteeDetails" \ "firstName").as[String]
          model.personalDetails.lastName mustBe (json \ "trusteeDetails" \ "lastName").as[String]
          model.personalDetails.dateOfBirth mustBe (json \ "dateOfBirth").as[String]
      }
    }

    "must read previous address when address years is under a year" in {
      forAll(trusteeIndividualGenerator(), false){
        (json, addressYears) =>
          val newJson  = json + ("addressYears" -> JsBoolean(addressYears))
          val model = newJson.as[Individual](Individual.readsTrusteeIndividual)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "previousAddress").as[Address]
      }
    }

    "must not read previous address when address years is not under a year" in {
      forAll(trusteeIndividualGenerator(), true){
        (json, addressYears) =>
          val newJson  = json + ("addressYears" -> JsBoolean(addressYears))
          val model = newJson.as[Individual](Individual.readsTrusteeIndividual)
          model.previousAddressDetails mustBe None
      }
    }

    "must read address" in {
      forAll(trusteeIndividualGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsTrusteeIndividual)
          model.correspondenceAddressDetails.addressDetails mustBe (json \ "address").as[Address]
      }
    }

    "must read contact details" in {
      forAll(trusteeIndividualGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsTrusteeIndividual)
          model.correspondenceContactDetails.contactDetails.email mustBe (json \ "emailAddress").as[String]
          model.correspondenceContactDetails.contactDetails.telephone mustBe (json \ "phoneNumber").as[String]
      }
    }

    "must read contact details removing white space from email address" in {
      forAll(trusteeIndividualGenerator(email = Some(" an email with white space "))) {
        json =>
          val model = json.as[Individual](Individual.readsTrusteeIndividual)
          model.correspondenceContactDetails.contactDetails.email mustBe "anemailwithwhitespace"
          model.correspondenceContactDetails.contactDetails.telephone mustBe (json \ "phoneNumber").as[String]
      }
    }

    "must read nino when it is present" in {
      forAll(trusteeIndividualGenerator(), arbitrary[String]){
        (json, nino) =>
          val newJson  = json + ("nino" -> Json.obj("value" -> nino))
          val model = newJson.as[Individual](Individual.readsTrusteeIndividual)
          model.referenceOrNino.value mustBe (newJson \ "nino" \ "value").as[String]
      }
    }

    "must read no nino reason when it is present" in {
      forAll(trusteeIndividualGenerator(), arbitrary[String]){
        (json, noNinoReason) =>
          val newJson  = json + ("noNinoReason" -> JsString(noNinoReason))
          val model = newJson.as[Individual](Individual.readsTrusteeIndividual)
          model.noNinoReason.value mustBe (newJson \ "noNinoReason").as[String]
      }
    }

    "must read utr when it is present" in {
      forAll(trusteeIndividualGenerator(), utrGeneratorFromUser){
        (json, utr) =>
          val newJson  = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[Individual](Individual.readsTrusteeIndividual)
          model.utr mustBe stripUtr(Some(utr))
      }
    }

    "must read no utr reason when it is present" in {
      forAll(trusteeIndividualGenerator(), arbitrary[String]){
        (json, noUtrReason) =>
          val newJson  = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[Individual](Individual.readsTrusteeIndividual)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }
  }
}
