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
import repositories.LockCacheRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class LockCacheController @Inject()(repository: LockCacheRepository,
                                    val authConnector: AuthConnector,
                                    cc: ControllerComponents,
                                    authAction: AuthAction
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  private def getPstr(implicit req: AuthRequest[_]) = req.headers.get("pstr").getOrElse(throw MissingHeadersException)

  def lock: Action[AnyContent] = authAction.async {
    implicit request =>
      repository.setLock(lock).map{
        case true => Ok
        case false => Conflict
      }
  }

  def getLock: Action[AnyContent] = authAction.async {
    implicit request =>
      repository.getLock(lock)
        .map {
          case Some(migrationLock) => Ok(Json.toJson(migrationLock))
          case None => NotFound
        }
  }

  def getLockOnScheme: Action[AnyContent] = authAction.async {
    implicit request =>
      request.headers.get("pstr") match {
        case Some(pstr) =>
          repository.getLockByPstr(pstr).flatMap {
            case Some(migrationLock) => Future.successful(Ok(Json.toJson(migrationLock)))
            case None => Future.successful(NotFound)
          }
        case _ => Future.failed(new BadRequestException("Bad Request without pstr"))
      }
  }

  def getLockByUser: Action[AnyContent] = authAction.async {
    implicit request =>
      repository.getLockByCredId(request.externalId).map {
        case Some(migrationLock) => Ok(Json.toJson(migrationLock))
        case None => NotFound
      }
  }

  def removeLock(): Action[AnyContent] = authAction.async { implicit request =>
      repository.releaseLock(lock).map(_ => Ok)
  }

  def removeLockOnScheme(): Action[AnyContent] = authAction.async { implicit request =>
    request.headers.get("pstr") match {
      case Some(pstr) => repository.releaseLockByPstr(pstr).map(_ => Ok)
      case _ => Future.failed(new BadRequestException("Bad Request without pstr"))
    }
  }

  def removeLockByUser(): Action[AnyContent] = authAction.async { implicit request =>
    repository.releaseLockByCredId(request.externalId).map(_ => Ok)
  }

  private def lock(implicit request: AuthRequest[AnyContent]) = {
    MigrationLock(getPstr, request.externalId, request.psaId)
  }

}
