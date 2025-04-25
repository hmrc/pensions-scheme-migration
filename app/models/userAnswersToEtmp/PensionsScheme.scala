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

package models.userAnswersToEtmp

import models.userAnswersToEtmp.establisher.EstablisherDetails
import models.userAnswersToEtmp.trustee.TrusteeDetails
import play.api.libs.functional.syntax._
import play.api.libs.json._
import models.userAnswersToEtmp.PensionSchemeDeclaration.formats

implicit val formatsPensionsScheme: Format[PensionsScheme] = Json.format[PensionsScheme]


case class PensionsScheme(schemeMigrationDetails:SchemeMigrationDetails,
                          customerAndSchemeDetails: CustomerAndSchemeDetails, pensionSchemeDeclaration: PensionSchemeDeclaration,
                          establisherDetails: EstablisherDetails, trusteeDetails: TrusteeDetails)

object PensionsScheme {

  implicit val formatsPensionsScheme: Format[PensionsScheme] = Json.format[PensionsScheme]

  def registerApiReads: Reads[PensionsScheme] = (
    SchemeMigrationDetails.reads and
    CustomerAndSchemeDetails.apiReads and
      PensionSchemeDeclaration.apiReads and
      EstablisherDetails.readsEstablisherDetails and
      TrusteeDetails.readsTrusteeDetails
    ) ((schemeMigrationDetails,custAndSchemeDetails, declaration, estDetails, trusteeDetails) =>
    PensionsScheme(schemeMigrationDetails,custAndSchemeDetails, declaration, estDetails, trusteeDetails)
  )
}
