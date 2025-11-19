/*
 * Copyright 2025 HM Revenue & Customs
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
import models.enumeration.JourneyType
import models.{EmailEvents, EmailIdentifiers, Opened}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.*
import play.api.mvc.Results.{BadRequest, Forbidden, Ok}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter}
import uk.gov.hmrc.domain.PsaId

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait AuditEmailStatus {

  implicit val ec: ExecutionContext
  protected val logger: Logger
  protected val auditService: AuditService
  protected val crypto: Encrypter & Decrypter

  protected def auditEmailStatus(journeyType: JourneyType.Name,
                               emailIdentifiers: EmailIdentifiers)
                              (implicit request: Request[JsValue]): Result =
    Try(PsaId(emailIdentifiers.psaId)) match {
      case Success(psaId) =>
        request.body.validate[EmailEvents].fold(
          _ => BadRequest("Bad request received for email call back event"),
          valid => {
            valid.events.filterNot(
              _.event == Opened
            ).foreach { event =>
              logger.debug(s"Email Audit event coming from $journeyType is $event")
              emailIdentifiers.pstrId match {
                case Some(pstrId) =>
                  auditService.sendEvent(EmailAuditEvent(psaId, pstrId, journeyType, event.event))
                case None =>
                  auditService.sendEvent(EmailAuditEventPsa(psaId, journeyType, event.event))
              }
            }
            Ok
          })
      case Failure(_) => Forbidden("Malformed PSAID")
    }

  protected def getIDs(encryptedPsaId: String, encryptedPstrId: String): EmailIdentifiers =
    EmailIdentifiers(
      psaId = crypto.decrypt(Crypted(encryptedPsaId)).value,
      pstrId = Some(crypto.decrypt(Crypted(encryptedPstrId)).value)
    )

  protected def getPSAID(encryptedPsaId: String): EmailIdentifiers =
    EmailIdentifiers(
      psaId = crypto.decrypt(Crypted(encryptedPsaId)).value,
      pstrId = None
    )
}
