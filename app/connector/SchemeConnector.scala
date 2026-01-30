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

package connector

import audit.{AuditService, ListOfLegacySchemesAuditEvent}
import com.google.inject.Inject
import config.AppConfig
import connector.utils.{HttpResponseHelper, InvalidPayloadHandler}
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HttpVerbs.POST
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

class SchemeConnector @Inject()(
                                 http: HttpClientV2,
                                 config: AppConfig,
                                 invalidPayloadHandler: InvalidPayloadHandler,
                                 headerUtils: HeaderUtils,
                                 auditService: AuditService
                               )
  extends HttpErrorFunctions
    with HttpResponseHelper
    with Logging {
  
  def listOfLegacySchemes(psaId: String)
                         (implicit ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val url: String = config.listOfSchemesUrl.format(psaId)
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader)
    
    logger.debug(s"Calling migration list of schemes API on IF with url $url")

    http
      .get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            val totalResults = (response.json \ "totalResults").asOpt[Int].getOrElse(0)
            auditService.sendEvent(ListOfLegacySchemesAuditEvent(psaId, response.status, totalResults, ""))
            logger.debug(s"Call to migration list of schemes API on IF was successful with response ${response.json}")
            Right(response.json)
          case _ =>
            logger.warn(s"Call to migration list of schemes API on IF failed with response ${response.status} and body ${response.body}")
            auditService.sendEvent(ListOfLegacySchemesAuditEvent(psaId, response.status, 0, response.body))
            Left(handleErrorResponse("GET", url, response))
        }
      } recoverWith {
      case e: GatewayTimeoutException =>
        logger.warn(s"Call to migration list of schemes API on IF failed with GatewayTimeoutException ${e.getMessage}")
        auditService.sendEvent(ListOfLegacySchemesAuditEvent(psaId, 504, 0, e.getMessage))
        throw UpstreamErrorResponse(e.getMessage, 504)
    }
  }

  def registerScheme(psaId: String, registerData: JsValue)
                    (implicit ec: ExecutionContext): Future[Either[HttpException, JsValue]] = {

    val url: String = config.schemeRegistrationIFUrl.format(psaId)
    val schemaPath: String = "/resources/schemas/schemeSubscriptionIF.json"
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader)
    
    logger.debug(s"[Register-Migration-Scheme--Outgoing-Payload] - ${registerData.toString()}")

    http
      .post(url"$url")
      .setHeader(headerUtils.integrationFrameworkHeader *)
      .withBody(registerData)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            (response.json \ "schemeReferenceNumber").validate[String] match {
              case JsSuccess(value, _) =>
                Right(response.json)
              case JsError(_) =>
                Left(HttpException(response.body, response.status))
            }
          case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
            invalidPayloadHandler.logFailures(schemaPath, registerData, url)
            Left(BadRequestException(badRequestMessage(POST, url, response.body)))
          case _ =>
            Left(handleErrorResponse(POST, url, response))
        }
      }
  }
}
