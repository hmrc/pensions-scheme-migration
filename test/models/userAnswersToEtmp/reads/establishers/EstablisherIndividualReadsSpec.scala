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

import models.userAnswersToEtmp.reads.CommonGenerator.establisherIndividualGenerator
import models.userAnswersToEtmp.{Address, Individual}
import org.scalacheck.{Gen, Shrink}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{JsBoolean, JsString, Json}
import utils.PensionSchemeGenerators
import utils.UtrHelper.stripUtr

class EstablisherIndividualReadsSpec extends AnyFreeSpec with Matchers with ScalaCheckDrivenPropertyChecks with OptionValues with PensionSchemeGenerators {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny
  private def arbitraryString: Gen[String] =  Gen.alphaStr suchThat (_.nonEmpty)

  "An establisher individual" - {

    "must read individual details" in {
      forAll(establisherIndividualGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsEstablisherIndividual)
          model.personalDetails.firstName mustBe (json \ "establisherDetails" \ "firstName").as[String]
          model.personalDetails.lastName mustBe (json \ "establisherDetails" \ "lastName").as[String]
          model.personalDetails.dateOfBirth mustBe (json \ "dateOfBirth").as[String]
      }
    }

    "must read previous address when address years is under a year" in {
      forAll(establisherIndividualGenerator(), false){
        (json, addressYears) =>
          val newJson  = json + ("addressYears" -> JsBoolean(addressYears))
          val model = newJson.as[Individual](Individual.readsEstablisherIndividual)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "previousAddress").as[Address]
      }
    }

    "must not read previous address when address years is not under a year" in {
      forAll(establisherIndividualGenerator(),true){
        (json, addressYears) =>
          val newJson  = json + ("addressYears" -> JsBoolean(addressYears))
          val model = newJson.as[Individual](Individual.readsEstablisherIndividual)
          model.previousAddressDetails mustBe None
      }
    }

    "must read address" in {
      forAll(establisherIndividualGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsEstablisherIndividual)
          model.correspondenceAddressDetails.addressDetails mustBe (json \ "address").as[Address]
      }
    }

    "must read contact details" in {
      forAll(establisherIndividualGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsEstablisherIndividual)
          model.correspondenceContactDetails.contactDetails.email mustBe (json  \ "emailAddress").as[String]
          model.correspondenceContactDetails.contactDetails.telephone mustBe (json \ "phoneNumber").as[String]
      }
    }

    "must read contact details removing white space from email address" in {
      forAll(establisherIndividualGenerator(email = Some(" an email with white space "))) {
        json =>
          val model = json.as[Individual](Individual.readsEstablisherIndividual)
          model.correspondenceContactDetails.contactDetails.email mustBe "anemailwithwhitespace"
          model.correspondenceContactDetails.contactDetails.telephone mustBe (json \ "phoneNumber").as[String]
      }
    }

    "must read nino when it is present" in {
      forAll(establisherIndividualGenerator(), arbitraryString){
        (json, nino) =>
          val newJson  = json + ("nino" -> Json.obj("value" -> nino))
          val model = newJson.as[Individual](Individual.readsEstablisherIndividual)
          model.referenceOrNino.value mustBe (newJson \ "nino" \ "value").as[String]
      }
    }

    "must read no nino reason when it is present" in {
      forAll(establisherIndividualGenerator(), arbitraryString){
        (json, noNinoReason) =>
          val newJson  = json + ("noNinoReason" -> JsString(noNinoReason))
          val model = newJson.as[Individual](Individual.readsEstablisherIndividual)
          model.noNinoReason.value mustBe (newJson \ "noNinoReason").as[String]
      }
    }

    "must read utr when it is present" in {
      forAll(establisherIndividualGenerator(), utrGeneratorFromUser){
        (json, utr) =>
          val newJson  = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[Individual](Individual.readsEstablisherIndividual)
          model.utr mustBe stripUtr(Some(utr))
      }
    }

    "must read no utr reason when it is present" in {
      forAll(establisherIndividualGenerator(), arbitraryString){
        (json, noUtrReason) =>
          val newJson  = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[Individual](Individual.readsEstablisherIndividual)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }
  }
}
