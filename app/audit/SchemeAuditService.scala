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

import models.userAnswersToEtmp.PensionsScheme
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
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
          payload = Some(schemeDetails)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        LegacySchemeDetailsAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = e.responseCode,
          payload = None
        )
      )
    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(
        LegacySchemeDetailsAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = e.statusCode,
          payload = None
        )
      )
    case Failure(e: HttpException) =>
      sendEvent(
        LegacySchemeDetailsAuditEvent(
          psaId = psaId,
          pstr = pstr,
          status = e.responseCode,
          payload = None
        )
      )

  }

  def sendSchemeSubscriptionEvent(psaId: String,pstr: String, pensionsScheme: PensionsScheme)
                                 (
                                   sendEvent: MigrationSchemeSubscription => Unit
                                 )
                                 (implicit request: RequestHeader, ec: ExecutionContext):
  PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Right(outputResponse)) =>
      sendEvent(translateSchemeSubscriptionEvent(psaId,pstr, pensionsScheme, Status.OK, Some(outputResponse)))

    case Success(Left(e)) =>
      sendEvent(translateSchemeSubscriptionEvent(psaId,pstr, pensionsScheme,  e.responseCode, None))

    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(translateSchemeSubscriptionEvent(psaId,pstr, pensionsScheme,  e.statusCode, None))

    case Failure(e: HttpException) =>
      sendEvent(translateSchemeSubscriptionEvent(psaId,pstr, pensionsScheme,  e.responseCode, None))
  }

  def translateSchemeSubscriptionEvent
  (psaId: String,pstr: String, pensionsScheme: PensionsScheme, status: Int, response: Option[JsValue]): MigrationSchemeSubscription = {
    MigrationSchemeSubscription(
      psaIdentifier = psaId,
      pstr =pstr,
      schemeType = translateSchemeType(pensionsScheme),
      hasIndividualEstablisher = pensionsScheme.establisherDetails.individual.nonEmpty,
      hasCompanyEstablisher = pensionsScheme.establisherDetails.companyOrOrganization.nonEmpty,
      hasPartnershipEstablisher = pensionsScheme.establisherDetails.partnership.nonEmpty,
      status = status,
      request = Json.toJson(pensionsScheme),
      response = response
    )
  }
  private def translateSchemeType(pensionsScheme: PensionsScheme) = {
    if (pensionsScheme.customerAndSchemeDetails.isSchemeMasterTrust) {
      Some(SchemeType.masterTrust)
    }
    else {
      pensionsScheme.customerAndSchemeDetails.schemeStructure.map {
        case "single" => SchemeType.singleTrust
        case "group" => SchemeType.groupLifeDeath
        case "corp" => SchemeType.bodyCorporate
        case _ => SchemeType.other
      }
    }
  }
}
