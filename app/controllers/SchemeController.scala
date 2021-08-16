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

package controllers

import com.google.inject.Inject
import connector.SchemeConnector
import connector.utils.HttpResponseHelper
import models.ListOfLegacySchemes
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ValidationUtils.genResponse

import scala.concurrent.{ExecutionContext, Future}

class SchemeController @Inject()(
                                  schemeConnector: SchemeConnector,
                                  cc: ControllerComponents
                                )(
                                  implicit ec: ExecutionContext
                                )
  extends BackendController(cc) with HttpResponseHelper {

  def listOfLegacySchemes: Action[AnyContent] = Action.async {
    implicit request => {
      val psaId = request.headers.get("psaId")

      psaId match {
        case Some(id) =>
          schemeConnector.listOfLegacySchemes(id).map {
            case Right(json) => Ok(Json.toJson(json.convertTo[ListOfLegacySchemes]))
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("Bad Request with missing PSAId"))
      }
    }
  }
}
