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
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{Cursor, ReadConcern}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import service.{IdList, RacDacRequest}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
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

  private implicit val dateFormats: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
  override def now: DateTime = DateTime.now

  override lazy val workItemFields: WorkItemFieldNames = new WorkItemFieldNames {
      val receivedAt = "receivedAt"
      val updatedAt = "updatedAt"
      val availableAt = "receivedAt"
      val status = "status"
      val id = "_id"
      val failureCount = "failureCount"
    }

  override val inProgressRetryAfterProperty: String = "racDacWorkItem.submission-poller.in-progress-retry-after"
  private val retryPeriod = inProgressRetryAfter.getMillis.toInt

  private lazy val ttl = servicesConfig.getDuration("racDacWorkItem.submission-poller.mongo.ttl").toSeconds
  override def indexes: Seq[Index] = super.indexes ++ Seq(
    Index(key = Seq("item.psaId" -> IndexType.Ascending), name = Some("psaIdIdx")),
    Index(key = Seq("receivedAt" -> IndexType.Ascending), name = Some("receivedAtTime"),
      options = BSONDocument("expireAfterSeconds" -> ttl))
  )

  def pushAll(racDacRequests: Seq[RacDacRequest]): Future[Either[Exception, Seq[WorkItem[RacDacRequest]]]] = {
    pushNew(racDacRequests, now, (_: RacDacRequest) => ToDo).map(item => Right(item)).recover {
      case exception: Exception =>
        logger.error(s"Error occured while pushing items to the queue: ${exception.getMessage}")
        Left(WorkItemProcessingException(s"push failed for request due to ${exception.getMessage}"))
    }
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

  def getAllRequestsByPsaId(psaId: String): Future[List[WorkItem[RacDacRequest]]] = {
    collection.find(BSONDocument("item.psaId" -> psaId), projection = Option.empty[JsObject]).cursor[WorkItem[RacDacRequest]]()
      .collect[List](-1, Cursor.FailOnError())
  }

  def getTotalNoOfRequestsByPsaId(psaId: String): Future[Long] = {
    val selector= Json.obj("item.psaId" -> psaId)
    collection.count(Some(selector), None, skip = 0, None, ReadConcern.Local)
  }

  def getNoOfFailureByPsaId(psaId: String): Future[Long] = {
    val selector= Json.obj("status" -> "permanently-failed", "item.psaId" -> psaId)
    collection.count(Some(selector), None, skip = 0, None, ReadConcern.Local)
  }

  def deleteAllRequestsForPsaId(psaId: String): Future[Boolean] = {
    val selector = BSONDocument("item.psaId" -> psaId)
    collection.delete.one(selector).map(_.ok)
  }

  override def pullOutstanding(failedBefore: DateTime, availableBefore: DateTime)(implicit ec: ExecutionContext):
  Future[Option[WorkItem[RacDacRequest]]] = {

    def getWorkItem(idList: IdList): Future[Option[WorkItem[RacDacRequest]]] = {
      collection.find(
        selector = Json.obj(workItemFields.id -> ReactiveMongoFormats.objectIdWrite.writes(idList._id)),
        projection = Option.empty[JsObject]
      ).one[WorkItem[RacDacRequest]]
    }

    val id = findNextItemId(failedBefore, availableBefore)
    id.map(_.map(getWorkItem)).flatMap(_.getOrElse(Future.successful(None)))
  }

  private def setStatusOperation(newStatus: ProcessingStatus, availableAt: Option[DateTime]): JsObject = {
    val fields = Json.obj(
      workItemFields.status -> newStatus,
      workItemFields.updatedAt -> now
    ) ++ availableAt.map(when => Json.obj(workItemFields.availableAt -> when)).getOrElse(Json.obj())

    val ifFailed =
      if (newStatus == Failed)
        Json.obj("$inc" -> Json.obj(workItemFields.failureCount -> 1))
      else Json.obj()

    Json.obj("$set" -> fields) ++ ifFailed
  }
  private def findNextItemId(failedBefore: DateTime, availableBefore: DateTime)(implicit ec: ExecutionContext) : Future[Option[IdList]] = {

    def findNextItemIdByQuery(query: JsObject)(implicit ec: ExecutionContext): Future[Option[IdList]] =
      findAndUpdate(
        query = query,
        update = setStatusOperation(InProgress, None),
        fetchNewObject = true,
        fields = Some(Json.obj(workItemFields.id -> 1))
      ).map(_.value.map(Json.toJson(_).as[IdList]))

    def todoQuery: JsObject =
      Json.obj(
        workItemFields.status -> ToDo,
        workItemFields.availableAt -> Json.obj("$lt" -> availableBefore)
      )

    def failedQuery: JsObject =
      Json.obj("$or" -> Seq(
        Json.obj(workItemFields.status -> Failed, workItemFields.updatedAt -> Json.obj("$lt" -> failedBefore),
          workItemFields.availableAt -> Json.obj("$lt" -> availableBefore)),
        Json.obj(workItemFields.status -> Failed, workItemFields.updatedAt -> Json.obj("$lt" -> failedBefore),
          workItemFields.availableAt -> Json.obj("$exists" -> false))
      ))


    def inProgressQuery: JsObject =
      Json.obj(
        workItemFields.status -> InProgress,
        workItemFields.updatedAt -> Json.obj("$lt" -> now.minus(inProgressRetryAfter))
      )

    findNextItemIdByQuery(failedQuery).flatMap {
      case None => findNextItemIdByQuery(todoQuery).flatMap {
        case None => findNextItemIdByQuery(inProgressQuery)
        case item => Future.successful(item)
      }
      case item => Future.successful(item)
    }
  }

  case class WorkItemProcessingException(message: String) extends Exception(message)
}
