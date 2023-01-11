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

  val schemeTypeReads: Reads[JsObject] = (__ \ Symbol("pensionSchemeStructure")).readNullable[String].flatMap {
    _.map {
      schemeType =>
        (__ \ Symbol("schemeType") \ Symbol("name")).json.put(
          JsString(SchemeType.nameWithValue(schemeType))
        )
    }.getOrElse((__ \ Symbol("schemeType") \ Symbol("name")).json.put(JsString(SchemeType.other.name)))
  }

  val beforeYouStartReads: Reads[JsObject] = (
    (__ \ Symbol("schemeName")).json.copyFrom((__ \ Symbol("schemeName")).json.pick) and
      schemeTypeReads and
      (addressTransformer.getCountry(__ \ Symbol("schemeEstablishedCountry"), __ \ Symbol("schemeEstablishedCountry"), countryOptions) orElse doNothing)
    ) reduce

  val aboutMembershipReads: Reads[JsObject] = (__ \ Symbol("currentSchemeMembers")).readNullable[String].flatMap {
    _.flatMap {
      SchemeMembers.tppsNameWithValue(_).map { member =>
        (__ \ Symbol("futureMembers")).json.put(
          JsString(member)
        )
      }
    }.getOrElse(doNothing)
  }

  val benefitsAndInsuranceReads: Reads[JsObject] = (
    (__ \ Symbol("securedBenefits")).json.copyFrom((__ \ Symbol("isSchemeBenefitsInsuranceCompany")).json.pick) and
      (__ \ Symbol("investmentRegulated")).json.copyFrom((__ \ Symbol("isRegulatedSchemeInvestment")).json.pick) and
      (__ \ Symbol("occupationalPensionScheme")).json.copyFrom((__ \ Symbol("isOccupationalPensionScheme")).json.pick)
    ) reduce

  val userAnswersSchemeDetailsReads: Reads[JsObject] = {
    val readsSchemeDetails: Reads[JsObject] = (
      beforeYouStartReads and
        aboutMembershipReads and
        benefitsAndInsuranceReads and
        (__ \ Symbol("racDac")).json.copyFrom((__ \ Symbol("racDac")).json.pick) and
        (__ \ Symbol("relationshipStartDate")).json.copyFrom((__ \ Symbol("relationshipStartDate")).json.pick) and
        (__ \ Symbol("schemeOpenDate")).json.copyFrom((__ \ Symbol("schemeOpenDate")).json.pick)
      ) reduce

    (__ \ Symbol("items")).read(Reads.seq(readsSchemeDetails)).map(_.head)
  }
}
