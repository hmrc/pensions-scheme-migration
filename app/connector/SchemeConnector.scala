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

package connector

import audit.{AuditService, ListOfLegacySchemesAuditEvent}
import com.google.inject.Inject
import config.AppConfig
import connector.utils.HttpResponseHelper
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import scala.concurrent.{ExecutionContext, Future}

class SchemeConnector @Inject()(
                              http: HttpClient,
                              config: AppConfig,
                              headerUtils: HeaderUtils,
                              auditService:AuditService
                            )
  extends HttpErrorFunctions
    with HttpResponseHelper {

  private val logger = Logger(classOf[SchemeConnector])

  def listOfLegacySchemes(psaId: String)
                         (implicit headerCarrier: HeaderCarrier,
                          ec: ExecutionContext,
                          request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val listOfSchemesUrl = config.listOfSchemesUrl.format(psaId)

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
      headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
    logger.debug(s"Calling migration list of schemes API on IF with url $listOfSchemesUrl")

    http.GET[HttpResponse](listOfSchemesUrl)(
      implicitly[HttpReads[HttpResponse]],
      implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext]
    ).map { response =>
      response.status match {
        case OK =>
          val totalResults = (response.json \ "totalResults").asOpt[Int].getOrElse(0)
          auditService.sendEvent(ListOfLegacySchemesAuditEvent(psaId, response.status, totalResults, ""))
          logger.debug(s"Call to migration list of schemes API on IF was successful with response ${response.json}")
          Right(response.json)
        case _ =>
          auditService.sendEvent(ListOfLegacySchemesAuditEvent(psaId, response.status, 0, response.body))
          Left(handleErrorResponse("GET", listOfSchemesUrl, response))
      }
    }
  }

  def registerScheme(
                      psaId: String,
                      registerData: JsValue
                    )(
                      implicit
                      headerCarrier: HeaderCarrier,
                      ec: ExecutionContext
                    ): Future[Either[HttpException, JsValue]] = {

    val url = config.schemeRegistrationIFUrl.format(psaId)

    logger.debug(s"[Register-Migration-Scheme--Outgoing-Payload] - ${registerData.toString()}")

    http.POST[JsValue, HttpResponse](url, registerData) map { response =>
      response.status match {
        case OK =>
          Right(response.json)
        case _ => Left(handleErrorResponse("POST", url, response))
      }
    }
  }
}
