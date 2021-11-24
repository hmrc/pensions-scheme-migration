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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HttpException, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class SchemeAuditService {

  def sendSchemeDetailsEvent(psaId: String,
                             pstr: String)
                            (sendEvent: LegacySchemeDetailsAuditEvent => Unit): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {


    case Success(Right(schemeDetails)) =>
      sendEvent(
        LegacySchemeDetailsAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = Status.OK,
          response = Json.stringify(schemeDetails)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        LegacySchemeDetailsAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = e.responseCode,
          response = e.message
        )
      )
    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(
        LegacySchemeDetailsAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = e.statusCode,
          response = e.message
        )
      )
    case Failure(e: HttpException) =>
      sendEvent(
        LegacySchemeDetailsAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = e.responseCode,
          response = e.message
        )
      )

  }

  def sendRACDACSchemeSubscriptionEvent(psaId: String,pstr:String, registerData: JsValue)
                                       (
                                         sendEvent: RacDacMigrationAuditEvent => Unit
                                       )
                                       (implicit ec: ExecutionContext):
  PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Right(outputResponse)) =>
      sendEvent(RacDacMigrationAuditEvent(psaId,pstr, Status.OK, registerData, Some(outputResponse)))

    case Success(Left(e)) =>
      sendEvent(RacDacMigrationAuditEvent(psaId,pstr, e.responseCode, registerData, None))

    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(RacDacMigrationAuditEvent(psaId,pstr, e.statusCode, registerData, None))

    case Failure(e: HttpException) =>
      sendEvent(RacDacMigrationAuditEvent(psaId,pstr, e.responseCode, registerData, None))
  }

  def sendSchemeSubscriptionEvent(psaId: String,pstr:String, registerData: JsValue)
                                       (
                                         sendEvent: SchemeMigrationAuditEvent => Unit
                                       )
                                       (implicit ec: ExecutionContext):
  PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Right(outputResponse)) =>
      sendEvent(SchemeMigrationAuditEvent(psaId,pstr, Status.OK, registerData, Some(outputResponse)))

    case Success(Left(e)) =>
      sendEvent(SchemeMigrationAuditEvent(psaId,pstr, e.responseCode, registerData, None))

    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(SchemeMigrationAuditEvent(psaId,pstr, e.statusCode, registerData, None))

    case Failure(e: HttpException) =>
      sendEvent(SchemeMigrationAuditEvent(psaId,pstr, e.responseCode, registerData, None))
  }
}
