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

package repositories

import com.google.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import service.RacDacRequest
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.workitem._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RacDacRequestsQueueRepository @Inject()(configuration: Configuration, reactiveMongoComponent: ReactiveMongoComponent, servicesConfig: ServicesConfig)
                                             (implicit val ec: ExecutionContext) extends
  WorkItemRepository[RacDacRequest, BSONObjectID](
    collectionName = configuration.get[String](path = "mongodb.migration-cache.racDac-work-item-queue.name"),
    mongo = reactiveMongoComponent.mongoConnector.db,
    itemFormat = RacDacRequest.workItemFormat,
    config = configuration.underlying
  ) {
  override def now: DateTime =
    DateTime.now

  override lazy val workItemFields: WorkItemFieldNames =
    new WorkItemFieldNames {
      val receivedAt = "receivedAt"
      val updatedAt = "updatedAt"
      val availableAt = "receivedAt"
      val status = "status"
      val id = "_id"
      val failureCount = "failureCount"
    }

  override val inProgressRetryAfterProperty: String =
    "racDacWorkItem.submission-poller.in-progress-retry-after"

  private val retryPeriod = inProgressRetryAfter.getMillis.toInt

  private val ttlIndexName: String = "receivedAtTime"

  private lazy val ttl = servicesConfig.getDuration("dms.submission-poller.mongo.ttl").toSeconds

  private val ttlIndex: Seq[Index] =
    Seq(Index(
      key = Seq("receivedAt" -> IndexType.Ascending),
      name = Some(ttlIndexName),
      options = BSONDocument("expireAfterSeconds" -> ttl)
    ))

  (for { _ <- ensureIndexes } yield { () }) recoverWith {
    case t: Throwable => Future.successful(logger.error(s"Error creating indexes on collection ${collection.name}", t))
  } andThen {
    case _ => CollectionDiagnostics.logCollectionInfo(collection)
  }

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] =
    Future.sequence(ttlIndex.map(collection.indexesManager.ensure(_)))

  def push(racDacRequest: RacDacRequest): Future[Either[Exception, WorkItem[RacDacRequest]]] =
    pushNew(racDacRequest, now, (_: RacDacRequest) => ToDo).map(item => Right(item)).recover {
      case exception: Exception => Left(WorkItemProcessingException(s"push failed for request due to $message"))
    }

  def pull: Future[Either[Exception, Option[WorkItem[RacDacRequest]]]] =
    pullOutstanding(failedBefore = now.minusMillis(retryPeriod), availableBefore = now)
      .map(workItem => Right(workItem)).recover {
      case exception: Exception => Left(WorkItemProcessingException(s"pull failed due to ${exception.getMessage}"))
    }

  def setProcessingStatus(id: BSONObjectID, status: ProcessingStatus
                         ): Future[Either[Exception, Boolean]] =
    markAs(id, status, Some(now.plusMillis(retryPeriod)))
      .map(result => Right(result)).recover {
      case exception: Exception => Left(WorkItemProcessingException(s"setting processing status for $id failed due to ${exception.getMessage}"))
    }

  def setResultStatus(id: BSONObjectID, status: ResultStatus): Future[Either[Exception, Boolean]] =
    complete(id, status).map(result => Right(result)).recover {
      case exception: Exception => Left(WorkItemProcessingException(s"setting completion status for $id failed due to ${exception.getMessage}"))
    }

  case class WorkItemProcessingException(message: String) extends Exception(message)
}
