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

package controllers

import com.google.inject.Inject
import connector.LegacySchemeDetailsConnector
import connector.utils.HttpResponseHelper
import play.api.mvc._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class LegacySchemeDetailsController @Inject()(
                                               legacySchemeDetailsConnector: LegacySchemeDetailsConnector,
                                               cc: ControllerComponents,
                                               authAction: actions.AuthAction
                                             )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with HttpResponseHelper {

  def getLegacySchemeDetails: Action[AnyContent] = authAction.async {
    implicit request =>
      val pstr = request.headers.get("pstr")

      pstr.map { pstr =>
        legacySchemeDetailsConnector.getSchemeDetails(request.psaId, pstr).map {
          case Right(json) => Ok(json)
          case Left(e) => result(e)
        }
      }.getOrElse(Future.failed(new BadRequestException("Bad Request with missing parameters PSAId or PSTR")))
  }
}