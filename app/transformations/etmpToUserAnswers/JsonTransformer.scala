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

import scala.language.postfixOps

trait JsonTransformer {

  val doNothing: Reads[JsObject] = {
    __.json.put(Json.obj())
  }

  def userAnswersIndividualDetailsReads(userAnswersPath: String): Reads[JsObject] =
    (__ \ Symbol("personDetails")).readNullable[JsObject].flatMap {
      _ =>
        (__ \ userAnswersPath \ Symbol("firstName")).json.copyFrom((__ \ Symbol("personDetails") \ Symbol("firstName")).json.pick) and
          (__ \ userAnswersPath \ Symbol("lastName")).json.copyFrom((__ \ Symbol("personDetails") \ Symbol("lastName")).json.pick) and
          dateOfBirthReads reduce
    } orElse doNothing

  val dateOfBirthReads: Reads[JsObject] = (__ \ Symbol("personDetails") \ Symbol("dateOfBirth")).readNullable[String].flatMap {
    _.map {
      case "9999-12-31" => doNothing
      case dateOfBirth => (__ \ Symbol("dateOfBirth")).json.put(JsString(dateOfBirth))
    } getOrElse doNothing
  } orElse doNothing

  def userAnswersNinoReads: Reads[JsObject] =
    (__ \ Symbol("nino")).readNullable[String].flatMap {
      case Some(nino) if !nino.equals("") =>
        (__ \ Symbol("hasNino")).json.put(JsBoolean(true)) and
          (__ \ Symbol("nino") \ Symbol("value")).json.put(JsString(nino)) reduce
      case _ =>
        (__ \ Symbol("noNinoReason")).readNullable[String].flatMap {
          _.map { noNinoReason =>
            (__ \ Symbol("hasNino")).json.put(JsBoolean(false)) and
              (__ \ Symbol("noNinoReason")).json.put(JsString(noNinoReason)) reduce
          } getOrElse doNothing
        } orElse doNothing
    } orElse doNothing


  def userAnswersContactDetailsReads: Reads[JsObject] =
    ((__ \ Symbol("email")).json.copyFrom((__ \ Symbol("correspContDetails") \ Symbol("email")).json.pick) orElse doNothing) and
      ((__ \ Symbol("phone")).json.copyFrom((__ \ Symbol("correspContDetails") \ Symbol("telephone")).json.pick) orElse doNothing) reduce

  def userAnswersCompanyDetailsReads: Reads[JsObject] =
    (__ \ Symbol("companyDetails") \ Symbol("companyName")).json.copyFrom((__ \ Symbol("comOrOrganisationName")).json.pick) orElse doNothing

  def userAnswersCrnReads: Reads[JsObject] =
    (__ \ Symbol("crnNumber")).readNullable[String].flatMap {
      case Some(crnNumber) if !crnNumber.equals("") =>
        (__ \ Symbol("haveCompanyNumber")).json.put(JsBoolean(true)) and
          (__ \ Symbol("companyNumber") \ Symbol("value")).json.put(JsString(crnNumber)) reduce
      case _ =>
        (__ \ Symbol("noCrnReason")).readNullable[String].flatMap {
          _.map { noCrnReason =>
            (__ \ Symbol("haveCompanyNumber")).json.put(JsBoolean(false)) and
              (__ \ Symbol("noCompanyNumberReason")).json.put(JsString(noCrnReason)) reduce
          } getOrElse doNothing
        } orElse doNothing
    } orElse doNothing

  def userAnswersVatReads: Reads[JsObject] =
    (__ \ Symbol("vatRegistrationNumber")).readNullable[String].flatMap {
      case Some(vatRegistrationNumber) if !vatRegistrationNumber.equals("") =>
        (__ \ Symbol("haveVat")).json.put(JsBoolean(true)) and
          (__ \ Symbol("vat") \ Symbol("value")).json.put(JsString(vatRegistrationNumber)) reduce
      case _ =>
        (__ \ Symbol("haveVat")).json.put(JsBoolean(false))
    } orElse doNothing

  def userAnswersPayeReads: Reads[JsObject] =
    (__ \ "payeReference").readNullable[String].flatMap {
      case Some(payeReference) if !payeReference.equals("") =>
        (__ \ Symbol("havePaye")).json.put(JsBoolean(true)) and
          (__ \ Symbol("paye") \ Symbol("value")).json.put(JsString(payeReference.replaceAll("/", ""))) reduce
      case _ =>
        (__ \ Symbol("havePaye")).json.put(JsBoolean(false))
    } orElse doNothing
}
