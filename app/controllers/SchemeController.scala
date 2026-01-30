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
import connector.utils.HttpResponseHelper
import models.MigrationType.isRacDac
import models.{ListOfLegacySchemes, MigrationType}
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import repositories.ListOfLegacySchemesCacheRepository
import service.PensionSchemeService
import uk.gov.hmrc.http.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ValidationUtils.genResponse

import scala.concurrent.{ExecutionContext, Future}

class SchemeController @Inject()(
                                  pensionSchemeService: PensionSchemeService,
                                  listOfLegacySchemesCacheRepository: ListOfLegacySchemesCacheRepository,
                                  cc: ControllerComponents,
                                  authAction: actions.AuthAction
                                )(
                                  implicit ec: ExecutionContext
                                )
  extends BackendController(cc) with HttpResponseHelper with Logging {

  def listOfLegacySchemes: Action[AnyContent] = authAction.async {
    implicit request =>
      pensionSchemeService.getListOfLegacySchemes(request.psaId).map {
        case Right(json) => Ok(Json.toJson(json.convertTo[ListOfLegacySchemes]))
        case Left(e) => result(e)
      }
  }

  def registerScheme(migrationType: MigrationType): Action[AnyContent] = authAction.async {
    implicit request =>
      val feJson: Option[JsValue] = request.body.asJson
      val checkRacDac: Boolean = isRacDac(migrationType)
      logger.debug(s"[PSA-Scheme-Migration-Incoming-Payload] $feJson for Migration Type: $checkRacDac")
      feJson.map { jsValue =>

        val registerSchemeCall: Future[Either[HttpException, JsValue]] =
          if (checkRacDac)
            pensionSchemeService.registerRacDac(request.psaId, jsValue)(implicitly, implicitly, Some(implicitly))
          else
            pensionSchemeService.registerScheme(request.psaId, jsValue)

        registerSchemeCall.map {
          case Right(json) =>
            Ok(json)
          case Left(e) =>
            result(e)
        }
      }.getOrElse {
        Future.failed(new BadRequestException("Bad Request without PSAId or request body"))
      }.recoverWith {
        recoverFromError
      }
  }

  def removeListOfLegacySchemesCache: Action[AnyContent] = authAction.async { implicit request =>
      listOfLegacySchemesCacheRepository.remove(request.psaId).map(_ => Ok)
  }

}
