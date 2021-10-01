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
import models.racDac.{RacDacHeaders, RacDacRequest, WorkItemRequest}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, RequestHeader}
import service.RacDacBulkSubmissionService
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class TestBulkRacDacController @Inject()(
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
          val seqRacDacRequest = jsValue.as[Seq[RacDacRequest]]
          val racDacRequests = seqRacDacRequest.map(racDacReq => WorkItemRequest(id, racDacReq, RacDacHeaders(hc(request))))
          service.enqueue(racDacRequests).map(_ => Accepted)
        case _ =>
          Future.failed(new BadRequestException("Missing Body or missing psaId in the header"))
      }
    }
  }

  def isRequestSubmitted: Action[AnyContent] = Action.async {
    implicit request => {
      val psaId = request.headers.get("psaId")
      psaId match {
        case Some(id) =>
          service.isRequestSubmitted(id).map(isSubmitted => Ok(Json.toJson(isSubmitted)))
        case _ =>
          Future.failed(new BadRequestException("Missing psaId in the header"))
      }
    }
  }

  def isAllFailed: Action[AnyContent] = Action.async {
    implicit request => {
      val psaId = request.headers.get("psaId")
      psaId match {
        case Some(id) =>
          service.isAllFailed(id).map(isFailed => Ok(Json.toJson(isFailed)))
        case _ =>
          Future.failed(new BadRequestException("Missing psaId in the header"))
      }
    }
  }
}
