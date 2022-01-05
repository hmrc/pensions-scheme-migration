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

package models.racDac

import play.api.libs.json.{Json, Reads, Writes}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

case class IdList(_id: BSONObjectID)

object IdList {
  implicit val read: Reads[IdList] = {
    implicit val objectIdReads: Reads[BSONObjectID] = ReactiveMongoFormats.objectIdRead
    Json.reads[IdList]
  }
  implicit val writes: Writes[IdList] = {
    implicit val objectIdReads: Writes[BSONObjectID] = ReactiveMongoFormats.objectIdWrite
    Json.writes[IdList]
  }
}
