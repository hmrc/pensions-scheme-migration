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
import play.api.Logging
import play.api.libs.json.*
import play.api.mvc.RequestHeader
import repositories.{DeclarationLockRepository, ListOfLegacySchemesCacheRepository}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, LockedException}
import utils.JSONPayloadSchemaValidator

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionSchemeService @Inject()(
                                      schemeConnector: SchemeConnector,
                                      auditService: AuditService,
                                      schemeAuditService: SchemeAuditService,
                                      declarationLockRepository: DeclarationLockRepository,
                                      listOfLegacySchemesCacheRepository: ListOfLegacySchemesCacheRepository,
                                      jsonPayloadSchemaValidator: JSONPayloadSchemaValidator
                                    )(implicit ec: ExecutionContext) extends Logging {

  val schemaPath = "/resources/schemas/api-1359-request-schema-4.0.0.json"

  def registerScheme(psaId: String, json: JsValue)
                    (implicit ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]] =
    json.validate[PensionsScheme](PensionsScheme.registerApiReads).fold(
      invalid = {
        errors =>
          logger.warn("Invalid pension scheme", JsResultException(errors))
          Future.failed(new BadRequestException("Invalid pension scheme"))
      },
      valid = {
        validPensionsScheme =>
          val pstr = validPensionsScheme.schemeMigrationDetails.pstrOrTpssId
          val registerData = Json.toJson(validPensionsScheme).as[JsObject]

          declarationLockRepository.insertLockData(pstr, psaId).flatMap { isAvailable =>

            if (isAvailable) {
              val validationResult: JsObject =
                jsonPayloadSchemaValidator.validateJsonPayload(schemaPath, registerData) match {
                  case Left(errors) =>
                    logger.warn(s"Error Registering Scheme because payload had validation errors:-\n${errors.headOption.getOrElse("")}")
                    throw new RuntimeException("Error Registering Scheme because payload had validation errors")
                  case Right(_) =>
                    logger.warn(s"Payload for registering scheme passed validation")
                    registerData
                }

              schemeConnector
                .registerScheme(psaId, validationResult)
                .andThen {
                  val auditRegisterData: JsObject =
                    Seq(
                      (__ \ "pensionSchemeDeclaration" \ "box6").json.prune,
                      (__ \ "pensionSchemeDeclaration" \ "box7").json.prune,
                      (__ \ "pensionSchemeDeclaration" \ "box8").json.prune,
                      (__ \ "pensionSchemeDeclaration" \ "box10").json.prune,
                      (__ \ "pensionSchemeDeclaration" \ "box11").json.prune
                    ).foldLeft(validationResult) {
                      (result, reads) =>
                        result.transform(reads).asOpt.get
                    }

                  schemeAuditService.sendSchemeSubscriptionEvent(psaId, pstr, auditRegisterData)(auditService.sendEvent)
                }
            } else {
              Future(Left(LockedException(s"Scheme locked with pstr $pstr and psaId $psaId")))
            }
          }
      }
    )

  def registerRacDac(psaId: String, json: JsValue, isBulk: Boolean = false)
                    (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: Option[RequestHeader] = None): Future[Either[HttpException, JsValue]] =
    json.validate[RACDACPensionsScheme](RACDACPensionsScheme.reads).fold(
      invalid = {
        errors =>
          logger.warn("Invalid pension scheme", JsResultException(errors))
          Future.failed(new BadRequestException("Invalid pension scheme"))
      },
      valid = {
        validRacDacPensionsScheme =>
          val registerData: JsObject =
            Json.toJson(validRacDacPensionsScheme).as[JsObject]
          val auditRegisterData: JsObject =
            registerData.transform((__ \ Symbol("racdacSchemeDeclaration")).json.prune).asOpt.get

          schemeConnector
            .registerScheme(psaId, registerData)
            .andThen {
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

  def getListOfLegacySchemes(psaId: String)
                            (implicit request: RequestHeader): Future[Either[HttpException, JsValue]] =
    listOfLegacySchemesCacheRepository.get(psaId).flatMap {
      case Some(response) =>
        Future.successful(Right(response))
      case _ =>
        getAndCacheListOfLegacySchemes(psaId)
    }

  private def getAndCacheListOfLegacySchemes(psaId: String)(implicit request: RequestHeader): Future[Either[HttpException, JsValue]] =
    schemeConnector.listOfLegacySchemes(psaId).flatMap {
      case Right(psaDetails) =>
        listOfLegacySchemesCacheRepository
          .upsert(psaId, psaDetails)
          .map(_ => Right(psaDetails))
      case Left(e) =>
        Future.successful(Left(e))
    }

  private case class Scheme(pstr: String)

  def isAssociated(psaId: PsaId, pstr: String)(implicit request: RequestHeader): Future[Boolean] = {

    implicit val schemeFormat: Format[Scheme] = Json.format

    getListOfLegacySchemes(psaId.id).map {
      case Left(error) =>
        throw new RuntimeException("Unable to retrieve list of legacy schemes", error)
      case Right(value) =>
        (value \ "items").asOpt[Seq[Scheme]].getOrElse(Seq()).exists(_.pstr == pstr)
    }
  }
}

