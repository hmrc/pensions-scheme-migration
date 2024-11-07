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

package controllers.cache

import com.google.inject.Inject
import controllers.actions.{AuthAction, AuthRequest}
import models.cache.MigrationLock
import play.api.libs.json.Json
import play.api.mvc._
import repositories.DataCacheRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{BadRequestException, UnauthorizedException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class DataCacheController @Inject()(repository: DataCacheRepository,
                                    val authConnector: AuthConnector,
                                    cc: ControllerComponents,
                                    authAction: AuthAction
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def save: Action[AnyContent] = authAction.async {
    implicit request =>
      val psaId = request.headers.get("psaId").getOrElse(throw MissingHeadersException)
      val lock = MigrationLock(pstr, request.externalId, psaId)
      request.body.asJson.map { jsValue =>
        repository.renewLockAndSave(lock, jsValue).map(_ => Ok)
      } getOrElse Future.successful(BadRequest)
  }

  def get: Action[AnyContent] = authAction.async {
    implicit request =>
      repository.get(pstr)
        .map {
          case Some(migrationData) => Ok(Json.toJson(migrationData))
          case None => NotFound
        }
  }

  def remove: Action[AnyContent] = authAction.async {
    implicit request =>
      repository.remove(pstr).map(_ => Ok)
  }

  private def pstr(implicit request: AuthRequest[_]): String = request.headers.get("pstr").getOrElse(throw MissingHeadersException)
}


case object MissingHeadersException extends BadRequestException("Missing pstr from headers")


