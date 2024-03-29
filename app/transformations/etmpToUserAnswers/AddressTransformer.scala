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

package transformations.etmpToUserAnswers

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.CountryOptions

import scala.language.postfixOps

class AddressTransformer extends JsonTransformer {

  private def getCommonAddressElements(userAnswersPath: JsPath, ifAddressPath: JsPath): Reads[JsObject] = {
    ((userAnswersPath \ Symbol("addressLine1")).json.copyFrom((ifAddressPath \ Symbol("addressLine1")).json.pick)
      orElse doNothing) and
      ((userAnswersPath \ Symbol("addressLine2")).json.copyFrom((ifAddressPath \ Symbol("addressLine2")).json.pick)
        orElse doNothing) and
      ((userAnswersPath \ Symbol("addressLine3")).json.copyFrom((ifAddressPath \ Symbol("addressLine3")).json.pick)
        orElse doNothing) and
      ((userAnswersPath \ Symbol("addressLine4")).json.copyFrom((ifAddressPath \ Symbol("addressLine4")).json.pick)
        orElse doNothing) reduce
  }

  def getAddress(userAnswersPath: JsPath, ifAddressPath: JsPath, countryOptions: CountryOptions): Reads[JsObject] = {
    getCommonAddressElements(userAnswersPath, ifAddressPath) and
      ((userAnswersPath \ Symbol("postcode")).json.copyFrom((ifAddressPath \ Symbol("postalCode")).json.pick)
        orElse doNothing) and
      (getCountry(userAnswersPath \ Symbol("country"), ifAddressPath \ Symbol("country"), countryOptions) orElse doNothing) reduce
  }

  def getCountry(userAnswersPath: JsPath, ifAddressPath: JsPath, countryOptions: CountryOptions): Reads[JsObject] = {
    (ifAddressPath).readNullable[String].flatMap {
      _.flatMap {
        countryOptions.getCountryCodeFromName(_).map {
          countryCode =>
            (userAnswersPath).json.put(JsString(countryCode))
        }
      } getOrElse (doNothing)
    } orElse doNothing
  }
}