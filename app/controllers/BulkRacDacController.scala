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

import akka.actor.ActorSystem
import audit.{AuditService, RacDacBulkMigrationTriggerAuditEvent}
import com.google.inject.Inject
import models.racDac.{RacDacHeaders, RacDacRequest, SessionIdNotFound, WorkItemRequest}
import play.api.libs.json.{JsBoolean, JsError, JsSuccess, Json}
import play.api.mvc._
import repositories.RacDacRequestsQueueEventsLogRepository
import service.RacDacBulkSubmissionService
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, SessionId}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.AuthUtil

import scala.concurrent.{ExecutionContext, Future}

class BulkRacDacController @Inject()(
                                      cc: ControllerComponents,
                                      service: RacDacBulkSubmissionService,
                                      auditService: AuditService,
                                      authUtil: AuthUtil,
                                      repository: RacDacRequestsQueueEventsLogRepository,
                                      system: ActorSystem
                                    )(
                                      implicit ec: ExecutionContext
                                    )
  extends BackendController(cc) {

  private val serviceUnavailable = "Queue Service Unavailable"

  private def withId(block: (String) => Future[Result])
                    (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    hc.sessionId match {
      case Some(sessionId) => block(sessionId.value)
      case _ => Future.failed(SessionIdNotFound())
    }
  }

  private def putAllItemsOnQueue(sessionId: String, psaId: String, seqRacDacRequest: Seq[RacDacRequest])(
    implicit request: RequestHeader, executionContext: ExecutionContext): Future[Status] = {
    val totalResults = seqRacDacRequest.size
    val racDacRequests = seqRacDacRequest.map(racDacReq => WorkItemRequest(psaId, racDacReq, RacDacHeaders(hc(request))))
    Thread.sleep(20000)
    val queueRequest = service.enqueue(racDacRequests).map {
      case true => Accepted
      case false => ServiceUnavailable
    }(executionContext)
    queueRequest.flatMap { result =>
      result match {
        case Accepted =>
          auditService.sendEvent(RacDacBulkMigrationTriggerAuditEvent(psaId, totalResults, ""))(implicitly, executionContext)
        case ServiceUnavailable =>
          auditService.sendEvent(RacDacBulkMigrationTriggerAuditEvent(psaId, 0, serviceUnavailable))(implicitly, executionContext)
      }
      repository.save(sessionId, Json.obj("status" -> result.header.status))(executionContext).map(_ => result)(executionContext)
    }(executionContext)
  }

  def migrateAllRacDac: Action[AnyContent] = Action.async {
    implicit request =>
      authUtil.doAuth { _ =>
        HeaderCarrierConverter.fromRequest(request).sessionId match {
          case Some(SessionId(sessionId)) =>
            val optionPsaId = request.headers.get("psaId")
            val feJson = request.body.asJson

            val bulkRacDacExecutionContext: ExecutionContext = system.dispatchers.lookup(id = "racDacWorkItem")

            def processItems: Future[Status] = Future {
              (optionPsaId, feJson) match {
                case (Some(psaId), Some(jsValue)) =>
                  jsValue.validate[Seq[RacDacRequest]] match {
                    case JsSuccess(seqRacDacRequest, _) =>
                      repository.remove(sessionId)(bulkRacDacExecutionContext).flatMap { _ =>
                        putAllItemsOnQueue(sessionId, psaId, seqRacDacRequest)(implicitly, bulkRacDacExecutionContext)
                      }(bulkRacDacExecutionContext)
                    case JsError(_) =>
                      val error = new BadRequestException(s"Invalid request received from frontend for rac dac migration")
                      auditService.sendEvent(RacDacBulkMigrationTriggerAuditEvent(psaId, 0, error.message))(implicitly, bulkRacDacExecutionContext)
                      Future.failed(error)
                  }
                case _ =>
                  val error = new BadRequestException("Missing Body or missing psaId in the header")
                  auditService.sendEvent(RacDacBulkMigrationTriggerAuditEvent(optionPsaId.getOrElse(""), 0, error.message
                  ))(implicitly, bulkRacDacExecutionContext)
                  Future.failed(error)
              }
            }(bulkRacDacExecutionContext).flatten

            repository.remove(sessionId)(bulkRacDacExecutionContext).map { _ =>
              processItems
              Ok
            }
          case _ => Future.failed(SessionIdNotFound())
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
    authUtil.doAuth { _ =>
      request.headers.get("psaId") match {
        case Some(id) => fn(id)
        case _ => Future.failed(new BadRequestException("Missing psaId in the header"))
      }
    }
  }
}
