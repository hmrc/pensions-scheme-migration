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

import com.google.inject.Inject
import models.enumeration.{SchemeMembers, SchemeType}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.CountryOptions

import scala.language.postfixOps

class SchemeDetailsTransformer @Inject()(
                                          addressTransformer: AddressTransformer,
                                          countryOptions: CountryOptions
                                        )
  extends JsonTransformer {

  val schemeTypeReads: Reads[JsObject] = (__ \ 'pensionSchemeStructure).readNullable[String].flatMap {
    _.map {
      schemeType =>
        (__ \ 'schemeType \ 'name).json.put(
          JsString(SchemeType.nameWithValue(schemeType))
        )
    }.getOrElse((__ \ 'schemeType \ 'name).json.put(JsString(SchemeType.other.name)))
  }

  val beforeYouStartReads: Reads[JsObject] = (
    (__ \ 'schemeName).json.copyFrom((__ \ 'schemeName).json.pick) and
      schemeTypeReads and
      (addressTransformer.getCountry(__ \ 'schemeEstablishedCountry, __ \ 'schemeEstablishedCountry, countryOptions) orElse doNothing)
    ) reduce

  val aboutMembershipReads: Reads[JsObject] = (__ \ 'currentSchemeMembers).readNullable[String].flatMap {
    _.flatMap {
      SchemeMembers.nameWithValue(_).map { member =>
        (__ \ 'futureMembers).json.put(
          JsString(member)
        )
      }
    }.getOrElse(doNothing)
  }

  val benefitsAndInsuranceReads: Reads[JsObject] = (
    (__ \ 'securedBenefits).json.copyFrom((__ \ 'isSchemeBenefitsInsuranceCompany).json.pick) and
      (__ \ 'investmentRegulated).json.copyFrom((__ \ 'isRegulatedSchemeInvestment).json.pick) and
      (__ \ 'occupationalPensionScheme).json.copyFrom((__ \ 'isOccupationalPensionScheme).json.pick)
    ) reduce

  val userAnswersSchemeDetailsReads: Reads[JsObject] = {
    val readsSchemeDetails: Reads[JsObject] = (
      beforeYouStartReads and
        aboutMembershipReads and
        benefitsAndInsuranceReads and
        (__ \ 'racDac).json.copyFrom((__ \ 'racDac).json.pick) and
        (__ \ 'relationshipStartDate).json.copyFrom((__ \ 'relationshipStartDate).json.pick) and
        (__ \ 'schemeOpenDate).json.copyFrom((__ \ 'schemeOpenDate).json.pick)
      ) reduce

    (__ \ 'items).read(Reads.seq(readsSchemeDetails)).map(_.head)
  }
}
