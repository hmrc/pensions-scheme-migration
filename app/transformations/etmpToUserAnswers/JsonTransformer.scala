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

trait JsonTransformer {

  val doNothing: Reads[JsObject] = {
    __.json.put(Json.obj())
  }

  def userAnswersIndividualDetailsReads(userAnswersPath: String): Reads[JsObject] =
    (__ \ userAnswersPath \ 'firstName).json.copyFrom((__ \ 'personDetails \ 'firstName).json.pick) and
    (__ \ userAnswersPath \ 'lastName).json.copyFrom((__ \ 'personDetails \ 'lastName).json.pick) and
    (__ \ 'dateOfBirth).json.copyFrom((__ \ 'personDetails \ 'dateOfBirth).json.pick) reduce

  def userAnswersNinoReads: Reads[JsObject] =
    (__ \ "nino").read[String].flatMap { _ =>
    (__ \ 'hasNino).json.put(JsBoolean(true)) and
      (__ \ 'nino).json.copyFrom((__ \ 'nino).json.pick) reduce
  } orElse {
    (__ \ 'hasNino).json.put(JsBoolean(false)) and
      (__ \ 'noNinoReason).json.copyFrom((__ \ 'noNinoReason).json.pick) reduce
  } orElse {
    doNothing
  }

  def userAnswersContactDetailsReads: Reads[JsObject] =
    (__ \ 'email).json.copyFrom((__ \ 'correspContDetails \ 'email).json.pick) and
      (__ \ 'phone).json.copyFrom((__ \ 'correspContDetails \ 'telephone).json.pick) reduce

  def userAnswersCompanyDetailsReads: Reads[JsObject] =
    (__ \ 'companyDetails \ 'companyName).json.copyFrom((__ \ 'comOrOrganisationName).json.pick)

  def userAnswersCrnReads: Reads[JsObject] =
    (__ \ "crnNumber").read[String].flatMap { _ =>
      (__ \ 'haveCompanyNumber).json.put(JsBoolean(true)) and
        (__ \ 'companyNumber \ 'value).json.copyFrom((__ \ 'crnNumber).json.pick) reduce
    } orElse {
      (__ \ 'haveCompanyNumber).json.put(JsBoolean(false)) and
        (__ \ 'noCompanyNumberReason).json.copyFrom((__ \ 'noCrnReason).json.pick) reduce
    } orElse {
      doNothing
    }

  def userAnswersVatReads: Reads[JsObject] =
    (__ \ "vatRegistrationNumber").read[String].flatMap { _ =>
      (__ \ 'haveVat).json.put(JsBoolean(true)) and
        (__ \ 'vat).json.copyFrom((__ \ 'vatRegistrationNumber).json.pick) reduce
    } orElse {
      (__ \ 'haveVat).json.put(JsBoolean(false))
    }

  def userAnswersPayeReads: Reads[JsObject] =
    (__ \ "payeReference").read[String].flatMap { _ =>
      (__ \ 'havePaye).json.put(JsBoolean(true)) and
        (__ \ 'paye).json.copyFrom((__ \ 'payeReference).json.pick) reduce
    } orElse {
      (__ \ 'havePaye).json.put(JsBoolean(false))
    }
}
