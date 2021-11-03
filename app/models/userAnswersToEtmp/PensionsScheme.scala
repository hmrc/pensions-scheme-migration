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

package models.userAnswersToEtmp

import models.userAnswersToEtmp.establisher.EstablisherDetails
import models.userAnswersToEtmp.trustee.TrusteeDetails
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, _}
import utils.Lens

case class PensionsScheme(customerAndSchemeDetails: CustomerAndSchemeDetails, pensionSchemeDeclaration: Declaration,
                          establisherDetails: EstablisherDetails, trusteeDetails: TrusteeDetails,
                          changeOfEstablisherOrTrustDetails: Option[Boolean] = None)

object PensionsScheme {

  implicit val formatsPensionsScheme: Format[PensionsScheme] = Json.format[PensionsScheme]

  def registerApiReads: Reads[PensionsScheme] = (
    CustomerAndSchemeDetails.apiReads and
      PensionSchemeDeclaration.apiReads and
      EstablisherDetails.readsEstablisherDetails and
      TrusteeDetails.readsTrusteeDetails and
      (JsPath \ "changeOfEstablisherOrTrustDetails").readNullable[Boolean]
    ) ((custAndSchemeDetails, declaration, estDetails, trusteeDetails, changeFlag) =>
    PensionsScheme(custAndSchemeDetails, declaration, estDetails, trusteeDetails, changeFlag)
  )

  def updateApiReads: Reads[PensionsScheme] = (
    CustomerAndSchemeDetails.updateReads and
      PensionSchemeUpdateDeclaration.reads and
      EstablisherDetails.readsEstablisherDetails and
      TrusteeDetails.readsTrusteeDetails and
      (JsPath \ "changeOfEstablisherOrTrustDetails").readNullable[Boolean]
    ) ((custAndSchemeDetails, declaration, estDetails, trusteeDetails, changeFlag) =>
    PensionsScheme(custAndSchemeDetails, declaration, estDetails, trusteeDetails, changeFlag)
  )

  def updateWrite(psaId: String): Writes[PensionsScheme] = (
    (JsPath \ "schemeDetails").write(CustomerAndSchemeDetails.updateWrites(psaId)) and
      (JsPath \ "pensionSchemeDeclaration").write(Declaration.writes) and
      (JsPath \ "establisherAndTrustDetailsType").write(updateWriteEstablisherAndTrustDetails)
    ) (schemeDetails => (
    schemeDetails.customerAndSchemeDetails,
    schemeDetails.pensionSchemeDeclaration,
    (schemeDetails.changeOfEstablisherOrTrustDetails.getOrElse(false),
      Some(schemeDetails.customerAndSchemeDetails.haveMoreThanTenTrustee.getOrElse(false)),
      schemeDetails.establisherDetails,
      getOptionalTrustee(schemeDetails.trusteeDetails)))
  )

  val updateWriteEstablisherAndTrustDetails: Writes[(
    Boolean, Option[Boolean], EstablisherDetails, Option[TrusteeDetails])] = {
    ((JsPath \ "changeOfEstablisherOrTrustDetails").write[Boolean] and
      (JsPath \ "haveMoreThanTenTrustees").writeNullable[Boolean] and
      (JsPath \ "establisherDetails").write(EstablisherDetails.updateWrites) and
      (JsPath \ "trusteeDetailsType").writeNullable(TrusteeDetails.updateWrites)
      ) (element => element)
  }

  private def getOptionalTrustee(trusteeDetails: TrusteeDetails): Option[TrusteeDetails] = {
    if (trusteeDetails.companyTrusteeDetail.isEmpty &&
      trusteeDetails.individualTrusteeDetail.isEmpty &&
      trusteeDetails.partnershipTrusteeDetail.isEmpty) None else Some(trusteeDetails)
  }

  val pensionSchemeHaveInvalidBank: Lens[PensionsScheme, Boolean] = new Lens[PensionsScheme, Boolean] {
    override def get: PensionsScheme => Boolean = pensionsScheme => pensionsScheme.customerAndSchemeDetails.haveInvalidBank

    override def set: (PensionsScheme, Boolean) => PensionsScheme =
      (pensionsScheme, haveInvalidBank) =>
        pensionsScheme.copy(
          customerAndSchemeDetails =
            pensionsScheme.customerAndSchemeDetails.copy(haveInvalidBank = haveInvalidBank)
        )
  }

}
