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

import play.api.http.Status
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HttpException, UpstreamErrorResponse}

import scala.util.{Failure, Success, Try}

class SchemeAuditService {

  def sendSchemeDetailsEvent(psaId: String,
                             pstr: String)
                            (sendEvent: SchemeDetailsAuditEvent => Unit): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {


    case Success(Right(schemeDetails)) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = Status.OK,
          payload = Some(schemeDetails)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = e.responseCode,
          payload = None
        )
      )
    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = e.statusCode,
          payload = None
        )
      )
    case Failure(e: HttpException) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = e.responseCode,
          payload = None
        )
      )

  }
}
