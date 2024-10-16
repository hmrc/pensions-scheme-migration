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

import audit._
import com.google.inject.Inject
import config.AppConfig
import connector.utils.HttpResponseHelper
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import transformations.etmpToUserAnswers.PsaSchemeDetailsTransformer
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}


class LegacySchemeDetailsConnector @Inject()(
                                              http: HttpClientV2,
                                              config: AppConfig,
                                              auditService: AuditService,
                                              schemeSubscriptionDetailsTransformer: PsaSchemeDetailsTransformer,
                                              schemeAuditService: SchemeAuditService,
                                              headerUtils: HeaderUtils
                                            )
  extends HttpResponseHelper with Logging {

  def getSchemeDetails(psaId: String, pstr: String)
                      (implicit ec: ExecutionContext, rh: RequestHeader): Future[Either[HttpException, JsObject]] = {
    val (url, hc) = (
      config.legacySchemeDetailsUrl.format(pstr, psaId),
      HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader)
    )

    logger.debug(s"Calling get scheme details API on IF with url $url and hc $hc")

    http.get(url"$url")(hc).execute[HttpResponse].map(response =>
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
