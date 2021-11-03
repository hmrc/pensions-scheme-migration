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

import scala.language.postfixOps

trait JsonTransformer {

  val doNothing: Reads[JsObject] = {
    __.json.put(Json.obj())
  }

  def userAnswersIndividualDetailsReads(userAnswersPath: String): Reads[JsObject] =
    (__ \ 'personDetails).readNullable[JsObject].flatMap {
      _ =>
        (__ \ userAnswersPath \ 'firstName).json.copyFrom((__ \ 'personDetails \ 'firstName).json.pick) and
          (__ \ userAnswersPath \ 'lastName).json.copyFrom((__ \ 'personDetails \  'lastName).json.pick) and
            (__ \ 'dateOfBirth).json.copyFrom((__ \ 'personDetails \ 'dateOfBirth).json.pick) reduce
    } orElse doNothing

  def userAnswersNinoReads: Reads[JsObject] =
    (__ \ 'nino).readNullable[String].flatMap {
      case Some(nino) if !nino.equals("") =>
        (__ \ 'hasNino).json.put(JsBoolean(true)) and
          (__ \ 'nino\ 'value).json.put(JsString(nino)) reduce
      case _ =>
        (__ \ 'noNinoReason).readNullable[String].flatMap {
          _.map { noNinoReason =>
            (__ \ 'hasNino).json.put(JsBoolean(false)) and
              (__ \ 'noNinoReason).json.put(JsString(noNinoReason)) reduce
          } getOrElse doNothing
        } orElse doNothing
    } orElse doNothing


  def userAnswersContactDetailsReads: Reads[JsObject] =
    ((__ \ 'email).json.copyFrom((__ \ 'correspContDetails \ 'email).json.pick) orElse doNothing) and
      ((__ \ 'phone).json.copyFrom((__ \ 'correspContDetails \ 'telephone).json.pick) orElse doNothing) reduce

  def userAnswersCompanyDetailsReads: Reads[JsObject] =
    (__ \ 'companyDetails \ 'companyName).json.copyFrom((__ \ 'comOrOrganisationName).json.pick) orElse doNothing

  def userAnswersCrnReads: Reads[JsObject] =
    (__ \ 'crnNumber).readNullable[String].flatMap {
      case Some(crnNumber) if !crnNumber.equals("") =>
        (__ \ 'haveCompanyNumber).json.put(JsBoolean(true)) and
          (__ \ 'companyNumber \ 'value).json.put(JsString(crnNumber)) reduce
      case _ =>
        (__ \ 'noCrnReason).readNullable[String].flatMap {
          _.map { noCrnReason =>
            (__ \ 'haveCompanyNumber).json.put(JsBoolean(false)) and
              (__ \ 'noCompanyNumberReason).json.put(JsString(noCrnReason)) reduce
          } getOrElse doNothing
        } orElse doNothing
    } orElse doNothing

  def userAnswersVatReads: Reads[JsObject] =
    (__ \ 'vatRegistrationNumber).readNullable[String].flatMap {
      case Some(vatRegistrationNumber) if !vatRegistrationNumber.equals("") =>
        (__ \ 'haveVat).json.put(JsBoolean(true)) and
          (__ \ 'vat\ 'value).json.put(JsString(vatRegistrationNumber)) reduce
      case _ =>
      (__ \ 'haveVat).json.put(JsBoolean(false))
    } orElse doNothing

  def userAnswersPayeReads: Reads[JsObject] =
    (__ \ "payeReference").readNullable[String].flatMap {
      case Some(payeReference) if !payeReference.equals("") =>
      (__ \ 'havePaye).json.put(JsBoolean(true)) and
        (__ \ 'paye\ 'value).json.put(JsString(payeReference)) reduce
      case _ =>
        (__ \ 'havePaye).json.put(JsBoolean(false))
    } orElse doNothing
}
