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

import play.api.libs.functional.syntax.toFunctionalBuilderOps

case class RACDACDeclaration(
                              box12: Boolean,
                              box13: Boolean,
                              box14: Boolean
                            )

object RACDACDeclaration {

  import play.api.libs.json._

  val reads: Reads[RACDACDeclaration] = (Reads.pure(true) and Reads.pure(true) and Reads.pure(true)) (RACDACDeclaration.apply _)

  implicit val formats: Format[RACDACDeclaration] = Json.format[RACDACDeclaration]

}
