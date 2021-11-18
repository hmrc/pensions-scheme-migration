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

package audit

import play.api.libs.json.{Reads, Writes}

object SchemeType extends Enumeration {
  val singleTrust = Value("Single trust")
  val groupLifeDeath = Value("Group life/death in service")
  val bodyCorporate = Value("Body corporate")
  val masterTrust = Value("Master trust")
  val other = Value("Other")

  implicit val reads: Reads[SchemeType.Value] = Reads.enumNameReads(SchemeType)
  implicit val writes: Writes[SchemeType.Value] = Writes.enumNameWrites

}
