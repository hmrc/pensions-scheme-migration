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

package models.userAnswersToEtmp

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, Json, Reads}

case class RACDACPensionsScheme(
                                 schemeMigrationDetails: SchemeMigrationDetails,
                                 racdacScheme: Boolean,
                                 racdacSchemeDetails: RACDACSchemeDetails,
                                 racdacSchemeDeclaration: RACDACDeclaration
                               )

object RACDACPensionsScheme {
  val reads: Reads[RACDACPensionsScheme] = (
    SchemeMigrationDetails.reads and
    RACDACSchemeDetails.reads and
      RACDACDeclaration.reads
    ) ((schemeMigrationDetails,racDACSchemeDetails, racDACDeclaration) =>
    RACDACPensionsScheme(
      schemeMigrationDetails,
      racdacScheme = true,
      racdacSchemeDetails = racDACSchemeDetails,
      racdacSchemeDeclaration = racDACDeclaration
    )
  )

  implicit val formats: Format[RACDACPensionsScheme] = Json.format[RACDACPensionsScheme]
}

