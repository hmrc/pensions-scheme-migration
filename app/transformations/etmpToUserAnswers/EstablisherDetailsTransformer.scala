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

class EstablisherDetailsTransformer @Inject()(addressTransformer: AddressTransformer, countryOptions: CountryOptions) extends JsonTransformer {

  val userAnswersEstablishersReads: Reads[JsObject] = {

    val readsIndividualOrCompanyObject: Reads[JsObject] = {
      val individualReads = (__ \ Symbol("individualDetails")).readNullable(userAnswersEstablisherIndividualReads)
      individualReads.flatMap {
        case None =>
          (__ \ Symbol("companyOrOrgDetails")).readNullable(userAnswersEstablisherCompanyReads).map {
            case None => Json.obj()
            case Some(jsObject) => jsObject
          }
        case Some(_) =>
          individualReads.map(_.getOrElse(Json.obj()))
      }
    }

    val establisherReads: Reads[Seq[JsObject]] =
      (__ \ Symbol("schemeEstablishers")).readNullable(Reads.seq(readsIndividualOrCompanyObject)).map(_.getOrElse(Nil))

    (__ \ Symbol("items")).readNullable(Reads.seq(establisherReads)).flatMap {
      case None => (__ \ Symbol("establishers")).json.put(Json.arr())
      case Some(items) =>
        val firstItem = Json.toJson(items.head)
        (__ \ Symbol("establishers")).json.put(firstItem)
    }
  }

  def userAnswersEstablisherIndividualReads: Reads[JsObject] = {
    (__ \ Symbol("establisherKind")).json.put(JsString("individual")) and
      userAnswersIndividualDetailsReads("establisherDetails") and
      userAnswersNinoReads and
      addressTransformer.getAddress(__ \ Symbol("address"), __ \ Symbol("correspAddrDetails"), countryOptions) and
      userAnswersContactDetailsReads reduce
  }

  def userAnswersEstablisherCompanyReads: Reads[JsObject] =
    (__ \ Symbol("establisherKind")).json.put(JsString("company")) and
      userAnswersCompanyDetailsReads and
      userAnswersVatReads and
      userAnswersPayeReads and
      userAnswersCrnReads and
      addressTransformer.getAddress(__ \ Symbol("address"), __ \ Symbol("correspAddrDetails"), countryOptions) and
      userAnswersContactDetailsReads reduce
}
