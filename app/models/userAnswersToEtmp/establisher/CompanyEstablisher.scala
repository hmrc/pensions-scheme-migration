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

import models.userAnswersToEtmp.ReadsHelper.{previousAddressDetails, readsFiltered}
import models.userAnswersToEtmp._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.UtrHelper.stripUtr

case class CompanyEstablisher(
                               organizationName: String,
                               utr: Option[String] = None,
                               noUtrReason: Option[String] = None,
                               crnNumber: Option[String] = None,
                               noCrnReason: Option[String] = None,
                               vatRegistrationNumber: Option[String] = None,
                               payeReference: Option[String] = None,
                               haveMoreThanTenDirectorOrPartner: Boolean,
                               correspondenceAddressDetails: CorrespondenceAddressDetails,
                               correspondenceContactDetails: CorrespondenceContactDetails,
                               previousAddressDetails: Option[PreviousAddressDetails] = None,
                               directorDetails: Seq[Individual]
                             )

object CompanyEstablisher {
  implicit val formats: Format[CompanyEstablisher] = Json.format[CompanyEstablisher]

  val readsEstablisherCompany: Reads[CompanyEstablisher] = (
    JsPath.read(Company.companyReads) and
      (JsPath \ "otherDirectors").readNullable[Boolean] and
      (JsPath \ "director").readNullable(
        readsFiltered(_ \ "directorDetails", Individual.readsCompanyDirector, "directorDetails")
      )
    ) ((company, otherDirectors, directors) =>
    CompanyEstablisher(
      organizationName = company.name,
      utr = stripUtr(company.utr),
      noUtrReason = company.noUtrReason,
      crnNumber = company.crn,
      noCrnReason = company.noCrnReason,
      vatRegistrationNumber = company.vatNumber,
      payeReference = company.payeNumber,
      haveMoreThanTenDirectorOrPartner = otherDirectors.getOrElse(false),
      correspondenceAddressDetails = CorrespondenceAddressDetails(company.address),
      correspondenceContactDetails = CorrespondenceContactDetails(company.contactDetails),
      previousAddressDetails = previousAddressDetails(company.addressYears, company.previousAddress, company.tradingTime),
      directorDetails = directors.getOrElse(Nil)
    )
  )
}
