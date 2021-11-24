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

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsPath, Json, Reads}

case class PersonalDetails(title: Option[String] = None, firstName: String, middleName: Option[String] = None,
                           lastName: String, dateOfBirth: String)

object PersonalDetails {
  implicit val formats: Format[PersonalDetails] = Json.format[PersonalDetails]

  def readsPersonDetails(userAnswersBase: String): Reads[PersonalDetails] =
    (
      (JsPath \ userAnswersBase \ "firstName").read[String] and
        (JsPath \ userAnswersBase \ "lastName").read[String] and
        (JsPath \ "dateOfBirth").read[String]
      ) ((firstName, lastName, date) => PersonalDetails(None, firstName, None, lastName, date))
}
