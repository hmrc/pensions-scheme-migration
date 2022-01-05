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

package controllers.cache

import com.google.inject.Inject
import play.api.mvc._
import repositories.SchemeDataCacheRepository
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class SchemeDataCacheController @Inject()(repository: SchemeDataCacheRepository,
                                    val authConnector: AuthConnector,
                                    cc: ControllerComponents
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def save: Action[AnyContent] = Action.async {
    implicit request =>
      withId { id =>
        request.body.asJson.map { jsValue =>
          repository.save(id, jsValue).map(_ => Ok)
        } getOrElse Future.successful(BadRequest)
      }
  }

  def get: Action[AnyContent] = Action.async {
    implicit request =>
      withId { id =>
        repository.get(id)
          .map {
            case Some(lock) => Ok(lock)
            case None => NotFound
          }
      }
  }

  def remove: Action[AnyContent] = Action.async {
    implicit request =>
      withId { id =>
        repository.remove(id).map(_ => Ok)
      }
  }

  private def withId(block: (String) => Future[Result])
                      (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    authorised().retrieve(Retrievals.externalId) {
      case Some(id) => block(id)
      case _ => Future.failed(CredIdNotFoundFromAuth())
    }
  }
}
