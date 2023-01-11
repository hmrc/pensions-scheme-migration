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
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.CountryOptions

import scala.language.postfixOps

class TrusteeDetailsTransformer @Inject()(addressTransformer: AddressTransformer, countryOptions: CountryOptions) extends JsonTransformer {


  val userAnswersTrusteesReads: Reads[JsObject] = {

    val readsIndividualOrCompanyObject: Reads[JsObject] = {
      val individualReads = (__ \ Symbol("individualDetails")).readNullable(userAnswersTrusteeIndividualReads)
      individualReads.flatMap {
        case None =>
          (__ \ Symbol("companyOrOrgDetails")).readNullable(userAnswersTrusteeCompanyReads).map {
            case None => Json.obj()
            case Some(jsObject) => jsObject
          }
        case Some(_) =>
          individualReads.map(_.getOrElse(Json.obj()))
      }
    }

    val trusteeReads: Reads[Seq[JsObject]] =
      (__ \ Symbol("schemeTrustees")).readNullable(Reads.seq(readsIndividualOrCompanyObject)).map(_.getOrElse(Nil))

    (__ \ Symbol("items")).readNullable(Reads.seq(trusteeReads)).flatMap {
      case None => (__ \ Symbol("trustees")).json.put(Json.arr())
      case Some(items) =>
        val firstItem = Json.toJson(items.head)
        (__ \ Symbol("trustees")).json.put(firstItem)
    }
  }

  def userAnswersTrusteeIndividualReads: Reads[JsObject] =
    (__ \ Symbol("trusteeKind")).json.put(JsString("individual")) and
      userAnswersIndividualDetailsReads("trusteeDetails") and
      userAnswersNinoReads and
      addressTransformer.getAddress(__ \ Symbol("address"), __ \ Symbol("correspAddrDetails"), countryOptions) and
      userAnswersContactDetailsReads reduce

  def userAnswersTrusteeCompanyReads: Reads[JsObject] =
    (__ \ Symbol("trusteeKind")).json.put(JsString("company")) and
      userAnswersCompanyDetailsReads and
      userAnswersVatReads and
      userAnswersPayeReads and
      userAnswersCrnReads and
      addressTransformer.getAddress(__ \ Symbol("address"), __ \ Symbol("correspAddrDetails"), countryOptions) and
      userAnswersContactDetailsReads reduce

}
