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


import audit.{AuditService, EmailAuditEvent, EmailAuditEventPsa}
import com.google.inject.Inject
import models.enumeration.JourneyType
import models.{EmailEvents, EmailIdentifiers, Opened}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.*
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, HttpException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class EmailResponseController @Inject()(
                                         auditService: AuditService,
                                         crypto: ApplicationCrypto,
                                         cc: ControllerComponents,
                                         parsers: PlayBodyParsers
                                       )(implicit ec: ExecutionContext)
  extends BackendController(cc) {
  private val logger = Logger(classOf[EmailResponseController])


  def retrieveStatus(journeyType: JourneyType.Name,
                     encryptedPsaId: Option[String] = None,
                     encryptedPstrId: Option[String] = None): Action[JsValue] = Action(parsers.tolerantJson) {
    implicit request =>
      
      getIDs(encryptedPsaId, encryptedPstrId) match {
        case Left(value) =>
          value match {
            case badRequestException: BadRequestException =>
              BadRequest(badRequestException.message)
            case exception =>
              throw exception
          }
        case Right(value) =>
          value match {
            case (psaId, pstrId) =>
              Try(PsaId(psaId)) match {
                case Success(psaId) =>
                  request.body.validate[EmailEvents].fold(
                    _ => BadRequest("Bad request received for email call back event"),
                    valid => {
                      valid.events.filterNot(
                        _.event == Opened
                      ).foreach { event =>
                        logger.debug(s"Email Audit event coming from $journeyType is $event")
                        auditService.sendEvent(EmailAuditEvent(psaId, pstrId, journeyType, event.event))
                      }
                      Ok
                    })
                case Failure(_) => Forbidden("Malformed PSAID")
              }
          }
      }
  }

  def retrieveStatusPsa(journeyType: JourneyType.Name, encryptedPsaId: String): Action[JsValue] = Action(parsers.tolerantJson) {
    implicit request =>
      val psaId = getPSAID(encryptedPsaId)
      Try(PsaId(psaId)) match {
        case Success(psaId) =>
          request.body.validate[EmailEvents].fold(
            _ => BadRequest("Bad request received for email call back event"),
            valid => {
              valid.events.filterNot(
                _.event == Opened
              ).foreach { event =>
                logger.debug(s"Email Audit event coming from $journeyType is $event")
                auditService.sendEvent(EmailAuditEventPsa(psaId, journeyType, event.event))
              }
              Ok
            })
        case Failure(_) => Forbidden("Malformed PSAID")
      }
  }

  private def getIDs(encryptedPsaId: Option[String],
                     encryptedPstrId: Option[String])(implicit request: Request[JsValue]): Either[HttpException,(String, String)] =
    (encryptedPsaId, encryptedPstrId) match {
      case (Some(psaId), Some(pstrId)) =>
        Right(
          crypto.QueryParameterCrypto.decrypt(Crypted(psaId)).value,
          crypto.QueryParameterCrypto.decrypt(Crypted(pstrId)).value
        )
      case _ =>
        request.body.validate[EmailIdentifiers] match {
          case JsSuccess(value, _) =>
            value match {
              case EmailIdentifiers(Some(psaId), Some(pstrId)) =>
                Right(psaId, pstrId)
              case EmailIdentifiers(None, Some(_)) =>
                Left(BadRequestException("Bad request received for email call back event: No psaId in request"))
              case EmailIdentifiers(Some(_), None) =>
                Left(BadRequestException("Bad request received for email call back event: No pstrId in request"))
              case _ =>
                Left(BadRequestException("Bad request received for email call back event: No pasId or pstrId in request"))
            }
          case JsError(errors) =>
            Left(BadRequestException(s"Bad request received for email call back event: ${errors.flatMap(_._2.map(_.messages.head)).mkString(",")}"))
        }
    }

  private def getPSAID(encryptedPsaId: String): String = {
    val psaId = crypto.QueryParameterCrypto.decrypt(Crypted(encryptedPsaId)).value
    psaId
  }

}
