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
import repositories.DataCacheRepository
import repositories.models.MigrationLock
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class DataCacheController @Inject()(repository: DataCacheRepository,
                                    val authConnector: AuthConnector,
                                    cc: ControllerComponents
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def save: Action[AnyContent] = Action.async {
    implicit request =>
      withPstr { (pstr, id) =>
        val psaId = request.headers.get("psaId").getOrElse(throw MissingHeadersException)
        val lock = MigrationLock(pstr, id, psaId)
        request.body.asJson.map { jsValue =>
          repository.renewLockAndSave(lock, jsValue).map(_ => Ok)
        } getOrElse Future.successful(BadRequest)
      }
  }

  def get: Action[AnyContent] = Action.async {
    implicit request =>
      withPstr { (pstr, _) =>
        repository.get(pstr)
          .map {
            case Some(migrationData) => Ok(Json.toJson(migrationData))
            case None => NotFound
          }
      }
  }

  def remove: Action[AnyContent] = Action.async {
    implicit request =>
      withPstr { (pstr, _) =>
        repository.remove(pstr).map(_ => Ok)
      }
  }

  private def withPstr(block: (String, String) => Future[Result])
                      (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    authorised().retrieve(Retrievals.externalId) {
      case Some(id) =>
        val pstr = request.headers.get("pstr").getOrElse(throw MissingHeadersException)
        block(pstr, id)
      case _ => Future.failed(CredIdNotFoundFromAuth())
    }
  }
  case object MissingHeadersException extends BadRequestException("Missing pstr from headers")

  case class CredIdNotFoundFromAuth(msg: String = "Not Authorised - Unable to retrieve credentials - externalId")
    extends UnauthorizedException(msg)
}
