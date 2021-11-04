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

package models.userAnswersToEtmp.establisher

import models.userAnswersToEtmp.ReadsHelper.readsFiltered
import models.userAnswersToEtmp._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.UtrHelper.stripUtr

case class Partnership(
                        organizationName: String,
                        utr: Option[String] = None,
                        noUtrReason: Option[String] = None,
                        vatRegistrationNumber: Option[String] = None,
                        payeReference: Option[String] = None,
                        haveMoreThanTenDirectorOrPartner: Boolean,
                        correspondenceAddressDetails: CorrespondenceAddressDetails,
                        correspondenceContactDetails: CorrespondenceContactDetails,
                        previousAddressDetails: Option[PreviousAddressDetails] = None,
                        partnerDetails: Seq[Individual]
                      )

object Partnership {
  implicit val formats: Format[Partnership] = Json.format[Partnership]

  val readsEstablisherPartnership: Reads[Partnership] = (
    JsPath.read(PartnershipDetail.partnershipReads) and
      (JsPath \ "otherPartners").readNullable[Boolean] and
      (JsPath \ "partner").readNullable(
        readsFiltered(_ \ "partnerDetails", Individual.readsPartner, "partnerDetails")
      )
    ) ((partnership, otherPartners, partners) =>
    Partnership(
      organizationName = partnership.name,
      utr = stripUtr(partnership.utr),
      noUtrReason = partnership.utrReason,
      vatRegistrationNumber = partnership.vat,
      payeReference = partnership.paye,
      haveMoreThanTenDirectorOrPartner = otherPartners.getOrElse(false),
      correspondenceAddressDetails = CorrespondenceAddressDetails(partnership.address),
      correspondenceContactDetails = CorrespondenceContactDetails(partnership.contact),
      previousAddressDetails = ReadsHelper.previousAddressDetails(partnership.addressYears, partnership.previousAddress, partnership.tradingTime),
      partnerDetails = partners.getOrElse(Nil)
    )
  )
}
