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

import com.google.inject.Inject
import config.AppConfig
import connector.utils.{HttpResponseHelper, UnrecognisedHttpResponseException}
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
                              headerUtils: HeaderUtils
                            )
  extends HttpErrorFunctions
    with HttpResponseHelper {

  private val logger = Logger(classOf[SchemeConnector])

  def listOfLegacySchemes(
                              psaId: String
                            )(
                              implicit
                              headerCarrier: HeaderCarrier,
                              ec: ExecutionContext
                            ): Future[Either[HttpException, JsValue]] = {
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
          logger.debug(s"Call to migration list of schemes API on IF was successful with response ${response.json}")
          Right(response.json)
        case _ => Left(handleErrorResponse("GET", listOfSchemesUrl, response))
      }
    }
  }

  //Todo: This is a placeholder for the new subscription api
  def registerRacDac(
                      psaId: String,
                      registerData: JsValue
                    )(
                      implicit
                      headerCarrier: HeaderCarrier,
                      ec: ExecutionContext
                    ): Future[Either[Exception, JsValue]] = {

    val url = config.racDacStubUrl.format(psaId)

    logger.debug(s"[Register-Rac Dac-Outgoing-Payload] - ${registerData.toString()}")

    http.POST[JsValue, HttpResponse](url, registerData) map { response =>
      response.status match {
        case OK =>
          Right(response.json)
        case _ => {
            val failureResponse = response.status match {
              case status if is4xx(status) =>
                UpstreamErrorResponse(
                  upstreamResponseMessage("Register rac dac", url, status, response.body), status, status, response.headers
                )
              case status if is5xx(status) =>
                UpstreamErrorResponse(
                  upstreamResponseMessage("Register rac dac", url, status, response.body), status, BAD_GATEWAY
                )
              case _ =>
                new UnrecognisedHttpResponseException("Register rac dac", url, response)
            }

          Left(failureResponse)
        }
      }
    }
  }
}