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

package models.userAnswersToEtmp.establisher

import models.userAnswersToEtmp.Individual
import models.userAnswersToEtmp.ReadsHelper.readsFilteredBoolean
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class EstablisherDetails(
                               individual: Seq[Individual],
                               companyOrOrganization: Seq[CompanyEstablisher],
                               partnership: Seq[Partnership]
                             )

object EstablisherDetails {
  implicit val formats: Format[EstablisherDetails] = Json.format[EstablisherDetails]

  private val isEstablisherKindIndividual: JsValue => Boolean = js =>
    (js \ "establisherKind").asOpt[String].contains( "individual") &&
      (js \ "establisherDetails" \ "firstName").asOpt[String].isDefined
  private val isEstablisherKindCompany: JsValue => Boolean = js =>
    (js \ "establisherKind").asOpt[String].contains("company") &&
      (js \ "companyDetails" \ "companyName").asOpt[String].isDefined
  private val isEstablisherKindPartnership: JsValue => Boolean = js =>
    (js \ "establisherKind").asOpt[String].contains("partnership") &&
      (js \ "partnershipDetails" \ "partnershipName").asOpt[String].isDefined

  val readsEstablisherDetails: Reads[EstablisherDetails] = (
    (JsPath \ "establishers").readNullable(
      readsFilteredBoolean( isEstablisherKindIndividual, Individual.readsEstablisherIndividual, "establisherDetails")
    ) and
      (JsPath \ "establishers").readNullable(
        readsFilteredBoolean(isEstablisherKindCompany, CompanyEstablisher.readsEstablisherCompany, "companyDetails")
      ) and
      (JsPath \ "establishers").readNullable(
        readsFilteredBoolean(isEstablisherKindPartnership, Partnership.readsEstablisherPartnership, "partnershipDetails"))
    ) ((establisherIndividuals, establisherCompanies, establisherPartnerships) =>
    EstablisherDetails(
      individual = establisherIndividuals.getOrElse(Nil),
      companyOrOrganization = establisherCompanies.getOrElse(Nil),
      partnership = establisherPartnerships.getOrElse(Nil)
    )
  )

}
