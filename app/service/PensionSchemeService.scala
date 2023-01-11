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

package service

import audit.{AuditService, SchemeAuditService}
import com.google.inject.{Inject, Singleton}
import connector.SchemeConnector
import models.userAnswersToEtmp.{PensionsScheme, RACDACPensionsScheme}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionSchemeService @Inject()(schemeConnector: SchemeConnector,
                                     auditService: AuditService,
                                     schemeAuditService: SchemeAuditService
                                    ) {

  private val logger = Logger(classOf[PensionSchemeService])

  def registerScheme(psaId: String, json: JsValue)
                    (implicit ec: ExecutionContext,
                     request: RequestHeader):
  Future[Either[HttpException, JsValue]] = {
    json.validate[PensionsScheme](PensionsScheme.registerApiReads).fold(
      invalid = {
        errors =>
          val ex = JsResultException(errors)
          logger.warn("Invalid pension scheme", ex)
          Future.failed(new BadRequestException("Invalid pension scheme"))
      },
      valid = {

        validPensionsScheme =>


          val registerData = Json.toJson(validPensionsScheme).as[JsObject]
          schemeConnector.registerScheme(psaId, registerData) andThen {

            val auditRegisterData = {
              val pathToBoxFields = Seq(
                (__ \ Symbol("pensionSchemeDeclaration") \ Symbol("box6")).json.prune,
                (__ \ Symbol("pensionSchemeDeclaration") \ Symbol("box7")).json.prune,
                (__ \ Symbol("pensionSchemeDeclaration") \ Symbol("box8")).json.prune,
                (__ \ Symbol("pensionSchemeDeclaration") \ Symbol("box10")).json.prune,
                (__ \ Symbol("pensionSchemeDeclaration") \ Symbol("box11")).json.prune)

              def pruneAll(jspaths: Seq[Reads[JsObject]], jsObject: JsObject): JsObject = {
                jspaths.foldLeft(jsObject) { (act, path) => act.transform(path).asOpt.get }
              }

              pruneAll(pathToBoxFields, registerData)
            }

            val pstr = validPensionsScheme.schemeMigrationDetails.pstrOrTpssId

            schemeAuditService.sendSchemeSubscriptionEvent(psaId, pstr, auditRegisterData)(auditService.sendEvent)
          }
      }
    )
  }

  def registerRacDac(psaId: String, json: JsValue, isBulk: Boolean = false)
                    (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: Option[RequestHeader] = None):
  Future[Either[HttpException, JsValue]] = {
    json.validate[RACDACPensionsScheme](RACDACPensionsScheme.reads).fold(
      invalid = {
        errors =>
          val ex = JsResultException(errors)
          logger.warn("Invalid pension scheme", ex)
          Future.failed(new BadRequestException("Invalid pension scheme"))
      },
      valid = {
        validRacDacPensionsScheme =>
          val registerData = Json.toJson(validRacDacPensionsScheme).as[JsObject]
          val auditRegisterData = registerData.transform((__ \ Symbol("racdacSchemeDeclaration")).json.prune).asOpt.get

          schemeConnector.registerScheme(psaId, registerData) andThen {
            val pstr = validRacDacPensionsScheme.schemeMigrationDetails.pstrOrTpssId
            if (isBulk) {
              schemeAuditService.sendRACDACSchemeSubscriptionEvent(psaId, pstr, auditRegisterData)(auditService.sendExplicitAudit)
            } else {
              implicit val requestHeader: RequestHeader = request.get
              schemeAuditService.sendRACDACSchemeSubscriptionEvent(psaId, pstr, auditRegisterData)(auditService.sendEvent)
            }
          }
      }
    )
  }
}

