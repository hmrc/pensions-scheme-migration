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
import controllers.actions.AuthAction
import models.racDac.SessionIdNotFound
import play.api.mvc._
import repositories.RacDacRequestsQueueEventsLogRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class RacDacRequestsQueueEventsLogController @Inject()(repository: RacDacRequestsQueueEventsLogRepository,
                                                       val authConnector: AuthConnector,
                                                       cc: ControllerComponents,
                                                       authAction: AuthAction
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {
  def getStatus: Action[AnyContent] = authAction.async {
    implicit request =>
      hc.sessionId match {
        case Some(sessionId) =>
          repository.get(sessionId.value)
            .map {
              case Some(jsValue) =>
                (jsValue \ "status").asOpt[Int] match {
                  case Some(status) =>

                    Results.Status(status)
                  case _ => NotFound
                }
              case None => NotFound
            }
        case _ => Future.failed(SessionIdNotFound())
      }

  }
}

