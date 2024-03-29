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

import com.google.inject.{Inject, Singleton}
import models.racDac.WorkItemRequest
import org.bson.types.ObjectId
import play.api.libs.json.{JsValue, Json}
import repositories.RacDacRequestsQueueRepository
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, ResultStatus, WorkItem}

import scala.concurrent.Future

@Singleton
class RacDacBulkSubmissionService @Inject()(
                                             racDacSubmissionRepo: RacDacRequestsQueueRepository,
                                             pensionSchemeService: PensionSchemeService
                                           )(implicit ec: RacDacBulkSubmissionPollerExecutionContext) {

  def submitToETMP(racDacRequest: WorkItemRequest): Future[Either[Exception, JsValue]] = {
    val psaId = racDacRequest.psaId
    val requestBody = Json.toJson(racDacRequest.request)
    val headerCarrier = racDacRequest.headers.toHeaderCarrier
    pensionSchemeService.registerRacDac(psaId, requestBody,isBulk = true)(headerCarrier, implicitly)
  }

  def enqueue(requests: Seq[WorkItemRequest]): Future[Boolean] = {
    requests match {
      case firstReq +: remainingReq =>
        racDacSubmissionRepo.push(firstReq) map {
        case Right(_) =>
          racDacSubmissionRepo.pushAll(remainingReq)
          true
        case _ =>
          false
      }
      case Nil => Future(false)
      case _ => Future(false)
    }
  }

  def dequeue: Future[Either[Exception, Option[WorkItem[WorkItemRequest]]]] =
    racDacSubmissionRepo.pull

  def setProcessingStatus(id: ObjectId, status: ProcessingStatus): Future[Either[Exception, Boolean]] =
    racDacSubmissionRepo.setProcessingStatus(id, status)

  def setResultStatus(id: ObjectId, status: ResultStatus): Future[Either[Exception, Boolean]] =
    racDacSubmissionRepo.setResultStatus(id, status)

  def isRequestSubmitted(psaId: String): Future[Either[Exception, Boolean]] = {
    racDacSubmissionRepo.getTotalNoOfRequestsByPsaId(psaId).map {
      case Right(noOfRequests) => Right(noOfRequests > 0)
      case Left(ex) => Left(ex)
    }
  }

  def isAllFailed(psaId: String): Future[Either[Exception, Option[Boolean]]] = {
    for {
      noOfRequestsE <- racDacSubmissionRepo.getTotalNoOfRequestsByPsaId(psaId)
      noOfFailuresE <- racDacSubmissionRepo.getNoOfFailureByPsaId(psaId)
    } yield {
      (noOfRequestsE, noOfFailuresE) match {
        case (Right(noOfRequests), Right(_)) if noOfRequests == 0 => Right(None)
        case (Right(noOfRequests), Right(noOfFailures)) => Right(Some(noOfRequests == noOfFailures))
        case _ => Left(new Exception("getting query for isAllFailed failed"))
      }
    }
  }

  def deleteRequest(id: ObjectId): Future[Boolean] = {
    racDacSubmissionRepo.deleteRequest(id)
  }

  def deleteAll(psaId: String): Future[Either[Exception, Boolean]] = {
    racDacSubmissionRepo.deleteAll(psaId)
  }
}
