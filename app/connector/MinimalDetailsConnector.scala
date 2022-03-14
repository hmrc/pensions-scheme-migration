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

package connector

import com.google.inject.ImplementedBy
import config.AppConfig
import connector.utils.HttpResponseHelper
import models.MinPSA
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MinimalDetailsConnectorImpl])
trait MinimalDetailsConnector {
  def getPSADetails(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, MinPSA]]
}

@Singleton
class MinimalDetailsConnectorImpl @Inject()(http: HttpClient, config: AppConfig)
  extends MinimalDetailsConnector with HttpResponseHelper {

  private val logger = Logger(classOf[MinimalDetailsConnectorImpl])

  private val delimitedErrorMsg: String = "DELIMITED_PSAID"

  def getPSADetails(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, MinPSA]] = {
    val url = config.getPSAMinDetails
    http.GET[HttpResponse](url)(implicitly, hc.withExtraHeaders("psaId" -> psaId), implicitly).map { response =>
      handleSchemeDetailsResponse(response, url)
    }
  }

  private def handleSchemeDetailsResponse(response: HttpResponse, url: String): Either[HttpException, MinPSA] = {
    logger.debug(s"Get-Psa-details-response - ${response.json}")
    response.status match {
      case OK =>
        Json.parse(response.body).validate[MinPSA] match {
          case JsSuccess(value, _) => Right(value)
          case JsError(errors) => throw JsResultException(errors)
        }
      case FORBIDDEN if response.body.contains(delimitedErrorMsg) => throw new DelimitedAdminException
      case _ =>
        Left(handleErrorResponse("GET", url, response))
    }
  }
}


class DelimitedAdminException extends
  Exception("The administrator has already de-registered. The minimal details API has returned a DELIMITED PSA response")
