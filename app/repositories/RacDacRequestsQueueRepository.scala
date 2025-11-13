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

package repositories

import com.google.inject.{Inject, Singleton}
import crypto.DataEncryptor
import models.racDac.{EncryptedWorkItemRequest, WorkItemRequest}
import org.bson.types.ObjectId
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes, Updates}
import play.api.libs.json.JsValue
import play.api.{Configuration, Logger}
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{PermanentlyFailed, ToDo}
import uk.gov.hmrc.mongo.workitem.*
import uk.gov.hmrc.mongo.{MongoComponent, MongoUtils}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RacDacRequestsQueueRepository @Inject()(configuration: Configuration,
                                              mongoComponent: MongoComponent,
                                              servicesConfig: ServicesConfig,
                                              dataEncryptor: DataEncryptor)
                                             (implicit val ec: ExecutionContext) extends
  WorkItemRepository[EncryptedWorkItemRequest](
    collectionName = configuration.get[String](path = "mongodb.migration-cache.racDac-work-item-queue.name"),
    mongoComponent = mongoComponent,
    itemFormat = implicitly,
    workItemFields = WorkItemFields.default
  ) {

  override def ensureIndexes(): Future[Seq[String]] = {
    val extraIndexes: Seq[IndexModel] = Seq(
      IndexModel(Indexes.ascending("item.psaId"), IndexOptions().name("psaIdIdx").background(true)),
      IndexModel(
        Indexes.ascending("receivedAt"), IndexOptions()
          .name("receivedAtTime")
          .background(true)
          .expireAfter(ttl, TimeUnit.SECONDS)
      )
    )
    MongoUtils.ensureIndexes(collection, indexes ++ extraIndexes, replaceIndexes = false)
  }

  private val logger: Logger = Logger(classOf[RacDacRequestsQueueRepository])

  override def now(): Instant = Instant.now()

  override lazy val inProgressRetryAfter: Duration =
    Duration.ofMillis(configuration.get[Long]("racDacWorkItem.submission-poller.in-progress-retry-after"))

  private val retryPeriod = inProgressRetryAfter.toMillis

  private lazy val ttl = servicesConfig.getDuration("racDacWorkItem.submission-poller.mongo.ttl").toSeconds

  def pushAll(racDacRequests: Seq[WorkItemRequest]): Future[Either[Exception, Seq[WorkItem[EncryptedWorkItemRequest]]]] =
    pushNewBatch(racDacRequests.map(_.encrypt(dataEncryptor)), now(), (_: EncryptedWorkItemRequest) => ToDo).map(item => Right(item)).recover {
      case exception: Exception =>
        logger.error(s"Error occurred while pushing items to the queue: ${exception.getMessage}")
        Left(WorkItemProcessingException(s"push failed for request due to ${exception.getMessage}"))
    }

  def push(racDacRequest: WorkItemRequest): Future[Either[Exception, WorkItem[EncryptedWorkItemRequest]]] =
    pushNew(racDacRequest.encrypt(dataEncryptor), now(), (_: EncryptedWorkItemRequest) => ToDo).map(item => Right(item)).recover {
      case exception: Exception =>
        logger.error(s"Error occurred while pushing items to the queue: ${exception.getMessage}")
        Left(WorkItemProcessingException(s"push failed for request due to ${exception.getMessage}"))
    }

  def pull: Future[Either[Exception, Option[WorkItem[WorkItemRequest]]]] =
    pullOutstanding(failedBefore = now().minusMillis(retryPeriod), availableBefore = now())
      .map { maybeWorkItem =>
        Right( maybeWorkItem.map { workItem =>
          workItem.copy(item = workItem.item.decrypt(dataEncryptor))
        })
      }
      .recover {
        case exception: Exception => Left(WorkItemProcessingException(s"pull failed due to ${exception.getMessage}"))
      }

  def setProcessingStatus(id: ObjectId,
                          status: ProcessingStatus
                         ): Future[Either[Exception, Boolean]] =
    markAs(id, status, Some(now()
      .plusMillis(retryPeriod)))
      .map(result => Right(result))
      .recover {
        case exception: Exception => Left(WorkItemProcessingException(s"setting processing status for $id failed due to ${exception.getMessage}"))
      }

  def setResultStatus(id: ObjectId, status: ResultStatus): Future[Either[Exception, Boolean]] =
    complete(id, status).map(result => Right(result)).recover {
      case exception: Exception => Left(WorkItemProcessingException(s"setting completion status for $id failed due to ${exception.getMessage}"))
    }

  def getTotalNoOfRequestsByPsaId(psaId: String): Future[Either[Exception, Long]] =
    collection.countDocuments(
      filter = Filters.and(
        Filters.eq("item.psaId", psaId)
      )
    ).toFuture()
      .map(Right(_))
      .recover {
        case exception: Exception => Left(WorkItemProcessingException(
          s"getting no of requests failed due to ${exception.getMessage}"))
      }

  def getNoOfFailureByPsaId(psaId: String): Future[Either[Exception, Long]] =
    collection.countDocuments(
      filter = Filters.and(
        Filters.eq(workItemFields.status, PermanentlyFailed.name),
        Filters.eq("item.psaId", psaId)
      )
    ).toFuture()
      .map(Right(_))
      .recover {
        case exception: Exception => Left(WorkItemProcessingException(
          s"getting no of failed requests failed due to ${exception.getMessage}"))
      }

  def deleteRequest(id: ObjectId): Future[Boolean] =
    collection.deleteOne(
      filter = Filters.eq(workItemFields.id, id)
    ).toFuture()
      .map(_ => true)

  def deleteAll(psaId: String): Future[Either[Exception, Boolean]] =
    collection.deleteOne(
      filter = Filters.eq("item.psaId", psaId)
    ).toFuture()
      .map(_ => Right(true))
      .recover {
        case exception: Exception => Left(WorkItemProcessingException(s"deleting all requests failed due to ${exception.getMessage}"))
      }

  def saveMigratedData(psaId: String, data: JsValue): Future[Boolean] = {
    val upsertOptions = new FindOneAndUpdateOptions().upsert(true)
    val idKey = "item.psaId"

    collection.findOneAndUpdate(
      filter = Filters.eq(idKey, psaId),
      update = Updates.combine(
        set(idKey, psaId),
        set("item.request", Codecs.toBson(dataEncryptor.encrypt(psaId, data))),
        set("updatedAt", Instant.now())
      ),
      upsertOptions
    ).toFuture()
      .map(_ => true)
  }

  private case class WorkItemProcessingException(message: String) extends Exception(message)
}
