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

package models.userAnswersToEtmp.reads

import models._
import models.userAnswersToEtmp.{AddressAndContactDetails, ContactDetails, PensionSchemeDeclaration, UkAddress}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues
import play.api.libs.json.{JsBoolean, JsString, Json}

class PensionSchemeDeclarationReadsSpec extends AnyWordSpec with Matchers with OptionValues with Samples {
  "A json payload containing declarations" should {

    "Map correctly to a Declaration model" when {
      val declaration = Json.obj(
        "workingKnowledge" -> JsBoolean(true),
        "schemeType" -> Json.obj("name" -> "trust")
      )

      "We have a declaration field" when {
        "It is true then boxes 6 7 and 8 are true" in {
          val result = declaration.as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
          result.box6 mustBe true
          result.box7 mustBe true
          result.box8 mustBe true
        }
      }

      "if we have a duties field" when {
        "It is true, then box10 will be true and box11 is None, addressAndContactDetails is None" in {
          val result = declaration.as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
          result.box10.value mustBe true
          result.box11 mustBe None
          result.addressAndContactDetails mustBe None
        }

        "It is false and containing 'adviser' details box11 is true, box10 is None and details populated" in {
          val adviserAddress = "adviserAddress" -> Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"),
            "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
            "postcode" -> JsString("NE1"), "country" -> JsString("GB"))

          val name = "John"
          val address = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "NE1")
          val contact = ContactDetails("07592113", None, None, "test@test.com")
          val declaration = Json.obj(
            "workingKnowledge" -> JsBoolean(false),
            "adviserName" -> JsString("John"),
            "adviserEmail" -> "test@test.com",
            "adviserPhone" -> "07592113",
            "schemeType" -> Json.obj("name" -> "trust")
          )
          val result = (declaration + adviserAddress).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)

          result.box10 mustBe None
          result.box11.value mustBe true
          result.pensionAdviserName mustBe Some(name)
          result.addressAndContactDetails mustBe Some(AddressAndContactDetails(address, contact))
        }

        "if we have a duties field and is false but no contact or name details then addressAndContactDetails be None" in {
          val result = (declaration + ("workingKnowledge" -> JsBoolean(false))).as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
          result.box11.value mustBe true
          result.box10 mustBe None
          result.addressAndContactDetails mustBe None
          result.pensionAdviserName mustBe None

        }

      }

      "we have a isSchemeMasterTrust field" when {
        "set as true" in {
          val declaration = Json.obj(
            "workingKnowledge" -> JsBoolean(true),
            "schemeType" -> Json.obj("name" -> "master")
          )

          val result = declaration.as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
          result.box3.value mustBe true
        }

        "set as None" in {
          val declaration = Json.obj(
            "workingKnowledge" -> JsBoolean(true),
            "schemeType" -> Json.obj("name" -> "trust")
          )
          val result = declaration.as[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
          result.box3 mustBe None
        }
      }
    }
  }
}
