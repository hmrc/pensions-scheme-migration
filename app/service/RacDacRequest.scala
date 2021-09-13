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

package service

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.http.{HeaderCarrier, RequestId, SessionId}
import uk.gov.hmrc.workitem.WorkItem

case class RacDacHeaders(requestId: Option[String], sessionId: Option[String]) {
  def toHeaderCarrier: HeaderCarrier = HeaderCarrier(requestId = requestId.map(RequestId), sessionId = sessionId.map(SessionId))
}

object RacDacHeaders {
  implicit val formats: OFormat[RacDacHeaders] = Json.format[RacDacHeaders]

  def apply(hc: HeaderCarrier): RacDacHeaders = new RacDacHeaders(hc.requestId.map(_.value), hc.sessionId.map(_.value))
}

case class Request(schemeName: String)

object Request {
  implicit val requestFormat: OFormat[Request] = Json.format
}

case class RacDacRequest(
                          psaId: String,
                          request: Request,
                          headers: RacDacHeaders
                        )

object RacDacRequest {

  implicit val racdacSubmissionRequestFormat: OFormat[RacDacRequest] = Json.format

  val workItemFormat: Format[WorkItem[RacDacRequest]] = WorkItem.workItemMongoFormat[RacDacRequest]
}

