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

import audit._
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import connector.utils.HttpResponseHelper
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import transformations.etmpToUserAnswers.PsaSchemeDetailsTransformer
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[LegacySchemeDetailsConnectorImpl])
trait LegacySchemeDetailsConnector {

  def getSchemeDetails(
                        psaId: String,
                        pstr: String
                      )(
                        implicit
                        headerCarrier: HeaderCarrier,
                        ec: ExecutionContext,
                        request: RequestHeader
                      ): Future[Either[HttpException, JsObject]]

}

class LegacySchemeDetailsConnectorImpl @Inject()(
                                     http: HttpClient,
                                     config: AppConfig,
                                     auditService: AuditService,
                                     schemeSubscriptionDetailsTransformer: PsaSchemeDetailsTransformer,
                                     schemeAuditService: SchemeAuditService,
                                     headerUtils: HeaderUtils
                                   )
  extends LegacySchemeDetailsConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[LegacySchemeDetailsConnector])

  case class SchemeFailedMapToUserAnswersException() extends Exception

  override def getSchemeDetails(
                                 psaId: String,
                                 pstr: String
                               )(
                                 implicit
                                 headerCarrier: HeaderCarrier,
                                 ec: ExecutionContext,
                                 request: RequestHeader
                               ): Future[Either[HttpException, JsObject]] = {
    val (url, hc) = (
      config.legacySchemeDetailsUrl.format(pstr, psaId),
      HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
    )

    logger.debug(s"Calling get scheme details API on IF with url $url and hc $hc")

    http.GET[HttpResponse](url)(implicitly, hc, implicitly).map(response =>
      handleSchemeDetailsResponse(response, url)
    ) andThen
      schemeAuditService.sendSchemeDetailsEvent(psaId, pstr)(auditService.sendEvent)
  }

  private def handleSchemeDetailsResponse(response: HttpResponse, url: String): Either[HttpException, JsObject] = {
    logger.debug(s"Get-Scheme-details-response from IF API - ${response.json}")
    response.status match {
      case OK =>
        response.json.transform(schemeSubscriptionDetailsTransformer.transformToUserAnswers) match {
          case JsSuccess(value, _) =>
            logger.debug(s"Get-Scheme-details-UserAnswersJson - $value")
            Right(value)
          case JsError(e) =>
            throw JsResultException(e)
        }
      case _ =>
        Left(handleErrorResponse("GET", url, response))
    }
  }
}
