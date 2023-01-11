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

package models.enumeration

import models.enumeration.binders.EnumPathBinder
import play.api.mvc.PathBindable

object JourneyType extends Enumeration {
  type Name = Value
  val SCHEME_MIG: JourneyType.Value = Value("SchemeMigration")
  val RACDAC_IND_MIG: JourneyType.Value = Value("RetirementOrDeferredAnnuityContractMigration")
  val RACDAC_BULK_MIG: JourneyType.Value = Value("RetirementOrDeferredAnnuityContractBulkMigration")

  implicit val journeyTypePathBinder: PathBindable[Name] = EnumPathBinder.pathBinder(this)
}