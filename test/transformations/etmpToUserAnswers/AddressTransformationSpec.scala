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

package transformations.etmpToUserAnswers

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json._

class AddressTransformationSpec extends TransformationSpec {

  val addressTransformer = new AddressTransformer

  "if payload containing an address" when {
    "transformed using getAddress" must {
      "map correctly to the user answers address" in {
        forAll(addressJsValueGen("correspAddrDetails", "address")) {
          address => {
            val (ifAddress, userAnswersExpectedAddress) = address
            lazy val transformedJson = ifAddress.transform(addressTransformer.getAddress(__ \ 'address, __ \ 'correspAddrDetails, countryOptions)).asOpt.value
            transformedJson mustBe userAnswersExpectedAddress
          }
        }
      }
    }
  }
}