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

package models.userAnswersToEtmp.trustee

import models.userAnswersToEtmp.ReadsHelper.previousAddressDetails
import models.userAnswersToEtmp.{Company, CorrespondenceAddressDetails, CorrespondenceContactDetails, PreviousAddressDetails}
import play.api.libs.json.*
import utils.UtrHelper.stripUtr
import utils.RemoveSpaces.*

case class CompanyTrustee(
                           organizationName: String,
                           utr: Option[String] = None,
                           noUtrReason: Option[String] = None,
                           crnNumber: Option[String] = None,
                           noCrnReason: Option[String] = None,
                           vatRegistrationNumber: Option[String] = None,
                           payeReference: Option[String] = None,
                           correspondenceAddressDetails: CorrespondenceAddressDetails,
                           correspondenceContactDetails: CorrespondenceContactDetails,
                           previousAddressDetails: Option[PreviousAddressDetails] = None
                         )

object CompanyTrustee {
  implicit val formats: Format[CompanyTrustee] = Json.format[CompanyTrustee]

  val readsTrusteeCompany: Reads[CompanyTrustee] =
    JsPath.read(Company.companyReads).map(test =>
      CompanyTrustee(
        organizationName = test.name,
        utr = stripUtr(test.utr),
        noUtrReason = test.noUtrReason,
        crnNumber = test.crn.removeSpaces(),
        noCrnReason = test.noCrnReason,
        vatRegistrationNumber = test.vatNumber.removeSpaces(),
        payeReference = test.payeNumber.removeSpaces(),
        correspondenceAddressDetails = CorrespondenceAddressDetails(test.address),
        correspondenceContactDetails = CorrespondenceContactDetails(test.contactDetails),
        previousAddressDetails = previousAddressDetails(test.addressYears, test.previousAddress))
    )

}
