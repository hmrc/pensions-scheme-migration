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

package repositories.models

import org.joda.time.DateTime
import play.api.libs.json.{Format, JsValue, Json, OFormat}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

case class DataJson(pstr: String, data: JsValue, lastUpdated: DateTime, expireAt: DateTime)

object DataJson {
  implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
  implicit val format: OFormat[DataJson] = Json.format[DataJson]
}