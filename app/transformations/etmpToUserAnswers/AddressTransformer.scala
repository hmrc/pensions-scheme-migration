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

package transformations.etmpToUserAnswers

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

class AddressTransformer extends JsonTransformer {

  private def getCommonAddressElements(ifAddressPath: JsPath): Reads[JsObject] = {
    (__ \ 'addressLine1).json.copyFrom((ifAddressPath \ 'addressLine1).json.pick) and
    (__ \ 'addressLine2).json.copyFrom((ifAddressPath \ 'addressLine2).json.pick) and
    ((__ \ 'addressLine3).json.copyFrom((ifAddressPath \ 'addressLine3).json.pick)
      orElse doNothing) and
      ((__ \ 'addressLine4).json.copyFrom((ifAddressPath \ 'addressLine4).json.pick)
        orElse doNothing) reduce
  }

  def getAddress(ifAddressPath: JsPath): Reads[JsObject] = {
    getCommonAddressElements(ifAddressPath) and
      ((__ \ 'postcode).json.copyFrom((ifAddressPath \ 'postalCode).json.pick)
        orElse doNothing) and
      (__ \ 'country).json.copyFrom((ifAddressPath \ 'country).json.pick) reduce
  }

}
