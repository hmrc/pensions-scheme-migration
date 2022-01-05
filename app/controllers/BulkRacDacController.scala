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

package controllers

import com.google.inject.Inject
import models.racDac.{RacDacHeaders, RacDacRequest, WorkItemRequest}
import play.api.libs.json.{JsBoolean, JsError, JsSuccess}
import play.api.mvc._
import service.RacDacBulkSubmissionService
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class BulkRacDacController @Inject()(
                                      cc: ControllerComponents,
                                      service: RacDacBulkSubmissionService
                                    )(
                                      implicit ec: ExecutionContext
                                    )
  extends BackendController(cc) {

  def migrateAllRacDac: Action[AnyContent] = Action.async {
    implicit request => {
      def hc(implicit request: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      val psaId = request.headers.get("psaId")
      val feJson = request.body.asJson

      (psaId, feJson) match {
        case (Some(id), Some(jsValue)) =>
          jsValue.validate[Seq[RacDacRequest]] match {
            case JsSuccess(seqRacDacRequest, _) =>
              val racDacRequests = seqRacDacRequest.map(racDacReq => WorkItemRequest(id, racDacReq, RacDacHeaders(hc(request))))
              service.enqueue(racDacRequests).map {
                case true => Accepted
                case false => ServiceUnavailable
              }
            case JsError(_) =>
              Future.failed(new BadRequestException(s"Invalid request received from frontend for rac dac migration"))
          }
        case _ =>
          Future.failed(new BadRequestException("Missing Body or missing psaId in the header"))
      }
    }
  }

  def isRequestSubmitted: Action[AnyContent] = Action.async {
    implicit request => {
      withPsa { psaId =>
        service.isRequestSubmitted(psaId).map {
          case Right(isSubmitted) => Ok(JsBoolean(isSubmitted))
          case _ => ServiceUnavailable
        }
      }
    }
  }

  def isAllFailed: Action[AnyContent] = Action.async {
    implicit request => {
      withPsa { psaId =>
        service.isAllFailed(psaId).map {
          case Right(None) => NoContent
          case Right(Some(isFailed)) => Ok(JsBoolean(isFailed))
          case _ => ServiceUnavailable
        }
      }
    }
  }

  def deleteAll: Action[AnyContent] = Action.async {
    implicit request => {
      withPsa { psaId =>
        service.deleteAll(psaId).map {
          case Right(isDeleted) => Ok(JsBoolean(isDeleted))
          case _ => ServiceUnavailable
        }
      }
    }
  }

  private def withPsa(fn: String => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    request.headers.get("psaId") match {
      case Some(id) => fn(id)
      case _ => Future.failed(new BadRequestException("Missing psaId in the header"))
    }
  }
}
