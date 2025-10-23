/*
 * Copyright 2025 HM Revenue & Customs
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

import models.userAnswersToEtmp.UkAddress
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class AddressWritesSpec extends AnyWordSpec with Matchers with OptionValues {

  "A UK Address with all fields populated" should {
    "create valid json" when {
      val validUkAddressAll = UkAddress(
        addressLine1 = "address 1",
        addressLine2 = Some("address 2"),
        addressLine3 = Some("address 3"),
        addressLine4 = Some("address 4"),
        countryCode = "GB",
        postalCode = "ZZ1 1ZZ"
      )

      val result: JsValue = Json.toJson(validUkAddressAll)

      "with addressLine 1" in {
        (result \ "line1").as[String] mustBe validUkAddressAll.addressLine1
      }

      "with addressLine 2" in {
        (result \ "line2").as[String] mustBe validUkAddressAll.addressLine2.get
      }

      "with addressLine 3" in {
        (result \ "line3").as[String] mustBe validUkAddressAll.addressLine3.get
      }

      "with addressLine 4" in {
        (result \ "line4").as[String] mustBe validUkAddressAll.addressLine4.get
      }

      "with countryCode" in {
        (result \ "countryCode").as[String] mustBe validUkAddressAll.countryCode
      }

      "with postcode" in {
        (result \ "postalCode").as[String] mustBe validUkAddressAll.postalCode
      }

      "with addressType" in {
        (result \ "addressType").as[String] mustBe "UK"
      }
    }
  }

  "A UK Address with only mandatory fields populated" should {
    "create valid json" when {
      val validUkAddressPartial = UkAddress(
        addressLine1 = "address 1",
        countryCode = "GB",
        postalCode = "ZZ1 1ZZ"
      )

      val result: JsValue = Json.toJson(validUkAddressPartial)

      "with addressline 1" in {
        (result \ "line1").as[String] mustBe validUkAddressPartial.addressLine1
      }

      "with countryCode" in {
        (result \ "countryCode").as[String] mustBe validUkAddressPartial.countryCode
      }

      "with postcode" in {
        (result \ "postalCode").as[String] mustBe validUkAddressPartial.postalCode
      }

      "with addressType" in {
        (result \ "addressType").as[String] mustBe "UK"
      }
    }
  }

  "A UK Address with postcode containing multiple spaces" should {
    "create valid json" when {
      val validUkAddressPartial = UkAddress(
        addressLine1 = "address 1",
        countryCode = "GB",
        postalCode = "ZZ1  1ZZ"
      )

      val result: JsValue = Json.toJson(validUkAddressPartial)

      "with addressline 1" in {
        (result \ "line1").as[String] mustBe validUkAddressPartial.addressLine1
      }

      "with countryCode" in {
        (result \ "countryCode").as[String] mustBe validUkAddressPartial.countryCode
      }

      "with postcode only containing 1 space" in {
        (result \ "postalCode").as[String] mustBe "ZZ1 1ZZ"
      }

      "with addressType" in {
        (result \ "addressType").as[String] mustBe "UK"
      }
    }
  }
}
