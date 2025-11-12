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

package controllers

import audit.{AuditService, EmailRequestAuditEvent, RacDacBulkMigrationTriggerAuditEvent}
import com.google.inject.Inject
import config.AppConfig
import connector.*
import connector.utils.HttpResponseHelper
import controllers.actions.AuthAction
import models.enumeration.JourneyType.RACDAC_BULK_MIG
import models.racDac.{RacDacHeaders, RacDacRequest, WorkItemRequest}
import org.apache.pekko.actor.ActorSystem
import play.api.Logger
import play.api.libs.json.*
import play.api.mvc.*
import repositories.RacDacRequestsQueueEventsLogRepository
import service.RacDacBulkSubmissionService
import uk.gov.hmrc.crypto.{ApplicationCrypto,PlainText}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, SessionId}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}

class BulkRacDacController @Inject()(appConfig: AppConfig,
                                     cc: ControllerComponents,
                                     service: RacDacBulkSubmissionService,
                                     auditService: AuditService,
                                     repository: RacDacRequestsQueueEventsLogRepository,
                                     system: ActorSystem,
                                     emailConnector: EmailConnector,
                                     minimalDetailsConnector: MinimalDetailsConnector,
                                     crypto: ApplicationCrypto,
                                     authAction: AuthAction
                                    )(
                                      implicit ec: ExecutionContext
                                    )
  extends BackendController(cc) with HttpResponseHelper {
  private val logger = Logger(classOf[BulkRacDacController])
  private val serviceUnavailable = "Queue Service Unavailable"

  private def putAllItemsOnQueueThenSendAuditEventAndEmail(sessionId: String, psaId: String, seqRacDacRequest: Seq[RacDacRequest])(
    implicit request: RequestHeader, hc: HeaderCarrier, executionContext: ExecutionContext): Future[Status] = {
    val totalResults = seqRacDacRequest.size
    val racDacRequests = seqRacDacRequest.map(racDacReq => WorkItemRequest(psaId, racDacReq, RacDacHeaders(hc)))
    val queueRequest = service.enqueue(racDacRequests).map {
      case true => Accepted
      case false => ServiceUnavailable
    }(executionContext)
    queueRequest.flatMap { result =>
      val futureEmailResult = result match {
        case Accepted =>
          auditService.sendEvent(RacDacBulkMigrationTriggerAuditEvent(psaId, totalResults, ""))(implicitly, executionContext)
          sendEmail(psaId)(request, hc, executionContext)
        case r@ServiceUnavailable =>
          auditService.sendEvent(RacDacBulkMigrationTriggerAuditEvent(psaId, 0, serviceUnavailable))(implicitly, executionContext)
          logger.warn("Unable to add messages to queue: service unavailable")
          Future.successful(r)
        case r@_ =>
          logger.warn("Unable to add messages to queue: unknown error")
          Future.successful(r)
      }
      futureEmailResult.flatMap { _ =>
        repository.save(sessionId, Json.obj("status" -> result.header.status))(executionContext)
          .map { _ => result }(executionContext)
      }(executionContext)
    }(executionContext)
  }

  def clearEventLogThenInitiateMigrationSelf: Action[AnyContent] = authAction.async { implicit request =>
    val hc = HeaderCarrierConverter.fromRequest(request)
    (hc.sessionId match {
      case Some(SessionId(sessionId)) =>
        val feJson = request.body.asJson
        val executionContext: ExecutionContext = system.dispatchers.lookup(id = "racDacWorkItem")
        feJson match {
          case Some(jsValue) =>
            jsValue.validate[Seq[RacDacRequest]] match {
              case JsSuccess(seqRacDacRequest, _) =>
                repository.remove(sessionId)(ec).map { _ =>
                  putAllItemsOnQueueThenSendAuditEventAndEmail(sessionId, request.psaId, seqRacDacRequest)(request, hc, executionContext)
                  Ok
                }
              case JsError(_) =>
                val error = new BadRequestException(s"Invalid request received from frontend for rac dac migration")
                auditService.sendEvent(RacDacBulkMigrationTriggerAuditEvent(request.psaId, 0, error.message))(request, executionContext)
                Future.failed(error)
            }
          case _ =>
            val error = new BadRequestException("Missing Body in the header")
            auditService.sendEvent(RacDacBulkMigrationTriggerAuditEvent(request.psaId, 0, error.message
            ))(request, ec)
            Future.failed(error)
        }

      case _ =>
        val error = new BadRequestException("Session ID not found - Unable to retrieve session ID")
        auditService.sendEvent(RacDacBulkMigrationTriggerAuditEvent("", 0, error.message
        ))(request, ec)
        Future.failed(error)
    }) recoverWith recoverFromError
  }

  private def sendEmail(psaId: String)(implicit requestHeader: RequestHeader,
                                       headerCarrier: HeaderCarrier, executionContext: ExecutionContext): Future[Result] = {
    logger.debug(s"Sending bulk migration email for $psaId")
    val transformExceptions: PartialFunction[Throwable, Future[EmailStatus]] = {
      case _: Throwable => Future.successful(EmailNotSent)
    }
    minimalDetailsConnector.getPSADetails(psaId)(headerCarrier, executionContext).flatMap {
      case Right(minPsa) =>
        val futureEmailStatus = emailConnector.sendEmail(
          emailAddress = minPsa.email,
          templateName = appConfig.bulkMigrationConfirmationEmailTemplateId,
          params = Map("psaName" -> minPsa.name),
          callbackUrl(psaId)
        )(headerCarrier, executionContext).recoverWith[EmailStatus](transformExceptions)(executionContext)

        futureEmailStatus.map {
          case EmailSent =>
            auditService.sendEvent(
              EmailRequestAuditEvent(psaId, RACDAC_BULK_MIG, minPsa.email, pstrId = "")
            )(requestHeader, executionContext)
            Ok
          case EmailNotSent =>
            logger.warn("Unable to send email - status not sent")
            Ok
        }(executionContext)
      case Left(e) => Future.successful(result(e))
    }(executionContext)
  }

  private def callbackUrl(psaId: String): String = {
    val encryptedPsa = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(psaId)).value, StandardCharsets.UTF_8.toString)
    s"${appConfig.baseUrlPensionsSchemeMigration}/pensions-scheme-migration/email-response/${RACDAC_BULK_MIG}/$encryptedPsa"
  }

  def isRequestSubmitted: Action[AnyContent] = authAction.async {
    implicit request => {
      service.isRequestSubmitted(request.psaId).map {
        case Right(isSubmitted) => Ok(JsBoolean(isSubmitted))
        case _ => ServiceUnavailable
      }
    }
  }

  def isAllFailed: Action[AnyContent] = authAction.async {
    implicit request => {
      service.isAllFailed(request.psaId).map {
        case Right(None) => NoContent
        case Right(Some(isFailed)) => Ok(JsBoolean(isFailed))
        case _ => ServiceUnavailable
      }
    }
  }

  def deleteAll: Action[AnyContent] = authAction.async {
    implicit request =>
      service.deleteAll(request.psaId).map {
        case Right(isDeleted) => Ok(JsBoolean(isDeleted))
        case _ => ServiceUnavailable
      }
  }
}
