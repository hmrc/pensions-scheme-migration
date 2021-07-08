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

package uk.gov.hmrc.cgtpropertydisposals.service

import java.util.{Base64, UUID}
import cats.data.EitherT
import cats.instances.either._
import cats.instances.future._
import cats.instances.list._
import cats.syntax.traverse._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.JsValue
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.cgtpropertydisposals.connectors.dms.GFormConnector
import uk.gov.hmrc.cgtpropertydisposals.models.Error
import uk.gov.hmrc.cgtpropertydisposals.models.dms._
import uk.gov.hmrc.cgtpropertydisposals.models.ids.CgtReference
import uk.gov.hmrc.cgtpropertydisposals.models.returns.CompleteReturn
import uk.gov.hmrc.cgtpropertydisposals.models.upscan.UpscanCallBack.UpscanSuccess
import uk.gov.hmrc.cgtpropertydisposals.repositories.dms.DmsSubmissionRepo
import uk.gov.hmrc.cgtpropertydisposals.service.dms.{DmsSubmissionPollerExecutionContext, DmsSubmissionRequest}
import uk.gov.hmrc.cgtpropertydisposals.service.upscan.UpscanService
import uk.gov.hmrc.cgtpropertydisposals.util.Logging
import uk.gov.hmrc.workitem.{ProcessingStatus, ResultStatus, WorkItem}

import scala.concurrent.Future

@ImplementedBy(classOf[DefaultDmsSubmissionService])
trait DmsSubmissionService {

  def enqueue(racDacScheme: JsValue): EitherT[Future, Error, WorkItem[JsValue]]

  def dequeue: EitherT[Future, Error, Option[WorkItem[JsValue]]]

  def setProcessingStatus(id: BSONObjectID, status: ProcessingStatus): EitherT[Future, Error, Boolean]

  def setResultStatus(id: BSONObjectID, status: ResultStatus): EitherT[Future, Error, Boolean]

  def submitToDms(
                   html: String,
                   formBundleId: String,
                   cgtReference: String,
                   completeReturn: String,
                   id: UUID
                 ): EitherT[Future, Error, EnvelopeId]

}

@Singleton
class DefaultDmsSubmissionService @Inject() (
                                              gFormConnector: GFormConnector,
                                              upscanService: UpscanService,
                                              dmsSubmissionRepo: DmsSubmissionRepo,
                                              configuration: Configuration
                                            )(implicit ec: DmsSubmissionPollerExecutionContext)
  extends DmsSubmissionService
    with Logging {

  def getDmsMetaConfig[A : Configs](key: String): A =
    configuration.underlying
      .get[A](s"dms.$key")
      .value

  val queue: String            = getDmsMetaConfig[String]("queue-name")
  val b64businessArea: String  = getDmsMetaConfig[String]("b64-business-area")
  val businessArea             = new String(Base64.getDecoder.decode(b64businessArea))
  val backScanEnabled: Boolean = getDmsMetaConfig[Boolean]("backscan.enabled")

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def submitToDms(
                            html: B64Html,
                            formBundleId: String,
                            cgtReference: CgtReference,
                            completeReturn: CompleteReturn,
                            id: UUID
                          ): EitherT[Future, Error, EnvelopeId] =
    for {
      attachments     <- EitherT.liftF(upscanService.downloadFilesFromS3(getUpscanSuccesses(completeReturn)))
      fileAttachments <- EitherT.fromEither[Future](attachments.sequence)
      envId           <- gFormConnector.submitToDms(
        DmsSubmissionPayload(
          html,
          fileAttachments,
          DmsMetadata(
            formBundleId,
            cgtReference.value,
            queue,
            businessArea,
            if (backScanEnabled) Some(true) else None
          )
        ),
        id
      )
    } yield envId

  private def getUpscanSuccesses(completeReturn: CompleteReturn): List[UpscanSuccess] = {
    val mandatoryEvidence = completeReturn.fold(
      _.yearToDateLiabilityAnswers.mandatoryEvidence,
      s => s.yearToDateLiabilityAnswers.fold(n => n.mandatoryEvidence, _.mandatoryEvidence),
      _.yearToDateLiabilityAnswers.mandatoryEvidence,
      _.yearToDateLiabilityAnswers.mandatoryEvidence,
      _.yearToDateLiabilityAnswers.mandatoryEvidence
    )

    val supportingEvidences = completeReturn
      .fold(
        _.supportingDocumentAnswers.evidences,
        _.supportingDocumentAnswers.evidences,
        _.supportingDocumentAnswers.evidences,
        _.supportingDocumentAnswers.evidences,
        _.supportingDocumentAnswers.evidences
      )
      .map(_.upscanSuccess)

    mandatoryEvidence.toList.map(_.upscanSuccess) ::: supportingEvidences
  }

  override def enqueue(
                        dmsSubmissionRequest: DmsSubmissionRequest
                      ): EitherT[Future, Error, WorkItem[DmsSubmissionRequest]] =
    dmsSubmissionRepo.set(dmsSubmissionRequest)

  override def dequeue: EitherT[Future, Error, Option[WorkItem[DmsSubmissionRequest]]] =
    dmsSubmissionRepo.get

  override def setProcessingStatus(id: BSONObjectID, status: ProcessingStatus): EitherT[Future, Error, Boolean] =
    dmsSubmissionRepo.setProcessingStatus(id, status)

  override def setResultStatus(id: BSONObjectID, status: ResultStatus): EitherT[Future, Error, Boolean] =
    dmsSubmissionRepo.setResultStatus(id, status)

}
