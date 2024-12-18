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
import controllers.actions.{AuthAction, AuthRequest, SchemeAuthAction}
import models.cache.MigrationLock
import play.api.libs.json.Json
import play.api.mvc._
import repositories.LockCacheRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class LockCacheController @Inject()(repository: LockCacheRepository,
                                    val authConnector: AuthConnector,
                                    cc: ControllerComponents,
                                    authAction: AuthAction,
                                    schemeAuthAction: SchemeAuthAction
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {


  private def schemeAuthAsync(block: (AuthRequest[AnyContent], String) => Future[Result]) = {
    authAction.async { implicit req =>
      req.headers.get("pstr") match {
        case Some(pstr) => schemeAuthAction(pstr).invokeBlock(req, block(_, pstr))
        case None => Future.successful(BadRequest("pstr header not present"))
      }
    }
  }

  def lock: Action[AnyContent] = schemeAuthAsync { case (req, pstr) =>
      repository.setLock(MigrationLock(pstr, req.externalId, req.psaId)).map {
        case true => Ok
        case false => Conflict
      }
  }

  def getLock: Action[AnyContent] = schemeAuthAsync { case (req, pstr) =>
    repository.getLock(MigrationLock(pstr, req.externalId, req.psaId))
      .map {
        case Some(migrationLock) => Ok(Json.toJson(migrationLock))
        case None => NotFound
      }
  }

  def getLockOnScheme: Action[AnyContent] = schemeAuthAsync { case (req, pstr) =>
    repository.getLockByPstr(pstr).flatMap {
      case Some(migrationLock) => Future.successful(Ok(Json.toJson(migrationLock)))
      case None => Future.successful(NotFound)
    }
  }

  def getLockByUser: Action[AnyContent] = authAction.async {
    implicit request =>
      repository.getLockByCredId(request.externalId).map {
        case Some(migrationLock) => Ok(Json.toJson(migrationLock))
        case None => NotFound
      }
  }

  def removeLock(): Action[AnyContent] = schemeAuthAsync { case (req, pstr) =>
      repository.releaseLock(MigrationLock(pstr, req.externalId, req.psaId)).map(_ => Ok)
  }

  def removeLockOnScheme(): Action[AnyContent] = schemeAuthAsync { case (req, pstr) =>
    repository.releaseLockByPstr(pstr).map(_ => Ok)
  }

  def removeLockByUser(): Action[AnyContent] = authAction.async { implicit request =>
    repository.releaseLockByCredId(request.externalId).map(_ => Ok)
  }

}
