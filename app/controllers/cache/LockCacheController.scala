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

package controllers.cache

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import repositories.LockCacheRepository
import repositories.models.MigrationLock
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class LockCacheController @Inject()(repository: LockCacheRepository,
                                    val authConnector: AuthConnector,
                                    cc: ControllerComponents
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def lock: Action[AnyContent] = Action.async {
    implicit request =>
      withLock { lock =>
        repository.setLock(lock).map{
          case true => Ok
          case false => Conflict
        }
      }
  }

  def getLock: Action[AnyContent] = Action.async {
    implicit request =>
      withLock { lock =>
        repository.getLock(lock)
          .map {
            case Some(migrationLock) => Ok(Json.toJson(migrationLock))
            case None => NotFound
          }
      }
  }

  def getLockOnScheme: Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        request.headers.get("pstr") match {
          case Some(pstr) =>
            repository.getLockByPstr(pstr).flatMap {
              case Some(migrationLock) => Future.successful(Ok(Json.toJson(migrationLock)))
              case None => Future.successful(NotFound)
            }
          case _ => Future.failed(new BadRequestException("Bad Request without pstr"))
        }
      }
  }

  def getLockByUser: Action[AnyContent] = Action.async {
    implicit request =>
      authorised().retrieve(Retrievals.externalId) {
        case Some(id) =>
          repository.getLockByCredId(id).map {
            case Some(migrationLock) => Ok(Json.toJson(migrationLock))
            case None => NotFound
          }
        case _ => Future.failed(CredIdNotFoundFromAuth())
      }
  }

  def removeLock(): Action[AnyContent] = Action.async {
    implicit request =>
      withLock { lock =>
        repository.releaseLock(lock).map(_ => Ok)
      }
  }

  def removeLockOnScheme(): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        request.headers.get("pstr") match {
          case Some(pstr) => repository.releaseLockByPstr(pstr).map(_ => Ok)
          case _ => Future.failed(new BadRequestException("Bad Request without pstr"))
        }
      }
  }

  def removeLockByUser(): Action[AnyContent] = Action.async {
    implicit request =>
      println("\n\n >>>>>>>>>>>>>>> 1")
      authorised().retrieve(Retrievals.externalId) {
        case Some(id) =>
          println("\n\n >>>>>>>>>>>>>>> 2")
          repository.releaseLockByCredId(id).map(_ => Ok)
        case _ =>
          println("\n\n >>>>>>>>>>>>>>> 3")
          Future.failed(CredIdNotFoundFromAuth())
      }
  }

  private def withLock(block: MigrationLock => Future[Result])
                      (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
    authorised().retrieve(Retrievals.externalId) {
      case Some(id) =>
        val pstr = request.headers.get("pstr").getOrElse(throw MissingHeadersException)
        val psaId = request.headers.get("psaId").getOrElse(throw MissingHeadersException)
        block(MigrationLock(pstr, id, psaId))
      case _ =>
        Future.failed(CredIdNotFoundFromAuth())
    }

}
