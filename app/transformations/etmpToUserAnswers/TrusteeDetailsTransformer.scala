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

import com.google.inject.Inject
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.CountryOptions

class TrusteeDetailsTransformer @Inject()(addressTransformer: AddressTransformer, countryOptions: CountryOptions) extends JsonTransformer {

  val userAnswersTrusteesReads: Reads[JsObject] = {
    (__ \ 'items \ 'schemeTrustees).readNullable(__.read(
      (__ \ 'individualDetails).readNullable(
        __.read(Reads.seq(userAnswersTrusteeIndividualReads)).map(JsArray(_))).flatMap { individual =>
        (__ \ 'companyOrOrgDetails).readNullable(
          __.read(Reads.seq(userAnswersTrusteeCompanyReads)).map(JsArray(_))).flatMap { company =>
          (__ \ 'trustees).json.put(individual.getOrElse(JsArray()) ++ company.getOrElse(JsArray()) ) orElse doNothing
        }
      })).map {
      _.getOrElse(Json.obj())
    }
  }

  def userAnswersTrusteeIndividualReads: Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("individual")) and
      userAnswersIndividualDetailsReads("trusteeDetails") and
      userAnswersNinoReads and
      addressTransformer.getAddress( __ \ 'address, __ \ 'correspAddrDetails, countryOptions) and
      userAnswersContactDetailsReads reduce

  def userAnswersTrusteeCompanyReads: Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("company")) and
      userAnswersCompanyDetailsReads and
      userAnswersVatReads and
      userAnswersPayeReads and
      userAnswersCrnReads and
      addressTransformer.getAddress( __ \ 'address, __ \ 'correspAddrDetails, countryOptions) and
      userAnswersContactDetailsReads reduce

}