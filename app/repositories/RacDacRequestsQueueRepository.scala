/*
 * Copyright 2022 HM Revenue & Customs
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



import config.AppConfig
import models.racDac.WorkItemRequest
import org.bson.types.ObjectId
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import play.api.libs.json._
import play.api.{Configuration, Logger}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{PermanentlyFailed, ToDo}
import uk.gov.hmrc.mongo.workitem._
import uk.gov.hmrc.mongo.{MongoComponent, MongoUtils}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/*
1) Error injecting constructor, java.lang.NoSuchMethodError: 'void uk.gov.hmrc.mongo.play.json.PlayMongoRepository
.<init>(
uk.gov.hmrc.mongo.MongoComponent,
java.lang.String,
play.api.libs.json.Format,
scala.collection.Seq,
scala.Option,
boolean,
scala.reflect.ClassTag,
scala.concurrent.ExecutionContext)
'
  at repositories.RacDacRequestsQueueRepository.<init>(RacDacRequestsQueueRepository.scala:38)

 */

@Singleton
class RacDacRequestsQueueRepository @Inject()(configuration: Configuration, mongoComponent: MongoComponent, servicesConfig: ServicesConfig)
                                             (implicit val ec: ExecutionContext) extends
  WorkItemRepository[WorkItemRequest](
    collectionName = configuration.get[String](path = "mongodb.migration-cache.racDac-work-item-queue.name"),
    mongoComponent = mongoComponent,
    itemFormat = WorkItemRequest.workItemRequestFormat,
    workItemFields = WorkItemFields.default
  ) {

  override def ensureIndexes: Future[Seq[String]] = {
    val extraIndexes: Seq[IndexModel] = Seq(
      IndexModel(Indexes.ascending("item.psaId"), IndexOptions().name("psaIdIdx").background(true)),
      IndexModel(
        Indexes.ascending("receivedAt"), IndexOptions()
          .name("receivedAtTime")
          .background(true)
          .expireAfter(ttl, TimeUnit.SECONDS)
      )
    )
    MongoUtils.ensureIndexes(collection, indexes ++ extraIndexes, replaceIndexes = true)
  }

  private val logger: Logger = Logger(classOf[RacDacRequestsQueueRepository])

  override def now: Instant = Instant.now()

//  private implicit val dateFormats: Format[DateTime] = MongoJodaFormats.dateTimeFormat

  override lazy val inProgressRetryAfter: Duration =
    Duration.ofMillis(configuration.get[Long]("racDacWorkItem.submission-poller.in-progress-retry-after"))
  private val retryPeriod = inProgressRetryAfter.toMillis

  private lazy val ttl = servicesConfig.getDuration("racDacWorkItem.submission-poller.mongo.ttl").toSeconds

  def pushAll(racDacRequests: Seq[WorkItemRequest]): Future[Either[Exception, Seq[WorkItem[WorkItemRequest]]]] = {
    pushNewBatch(racDacRequests, now, (_: WorkItemRequest) => ToDo).map(item => Right(item)).recover {
      case exception: Exception =>
        logger.error(s"Error occurred while pushing items to the queue: ${exception.getMessage}")
        Left(WorkItemProcessingException(s"push failed for request due to ${exception.getMessage}"))
    }
  }

  def push(racDacRequest: WorkItemRequest): Future[Either[Exception, WorkItem[WorkItemRequest]]] = {
    pushNew(racDacRequest, now, (_: WorkItemRequest) => ToDo).map(item => Right(item)).recover {
      case exception: Exception =>
        logger.error(s"Error occurred while pushing items to the queue: ${exception.getMessage}")
        Left(WorkItemProcessingException(s"push failed for request due to ${exception.getMessage}"))
    }
  }

  def pull: Future[Either[Exception, Option[WorkItem[WorkItemRequest]]]] =
    pullOutstanding(failedBefore = now.minusMillis(retryPeriod), availableBefore = now)
      .map(workItem => Right(workItem)).recover {
      case exception: Exception => Left(WorkItemProcessingException(s"pull failed due to ${exception.getMessage}"))
    }

  def setProcessingStatus(id: ObjectId, status: ProcessingStatus
                         ): Future[Either[Exception, Boolean]] =
    markAs(id, status, Some(now.plusMillis(retryPeriod)))
      .map(result => Right(result)).recover {
      case exception: Exception => Left(WorkItemProcessingException(s"setting processing status for $id failed due to ${exception.getMessage}"))
    }

  def setResultStatus(id: ObjectId, status: ResultStatus): Future[Either[Exception, Boolean]] =
    complete(id, status).map(result => Right(result)).recover {
      case exception: Exception => Left(WorkItemProcessingException(s"setting completion status for $id failed due to ${exception.getMessage}"))
    }

  def getTotalNoOfRequestsByPsaId(psaId: String): Future[Either[Exception, Long]] = {
    collection.countDocuments(
      filter = Filters.and(
        Filters.eq("item.psaId", psaId)
      )
    ).toFuture().map(Right(_)).recover {
      case exception: Exception => Left(WorkItemProcessingException(
        s"getting no of requests failed due to ${exception.getMessage}"))
    }
    //    val selector = Json.obj("item.psaId" -> psaId)
    //    collection.count(Some(selector), None, skip = 0, None, ReadConcern.LOCAL).map(Right(_)).recover {
    //      case exception: Exception => {
    //        logger.error(s"getting total no of requests failed due to ${exception.getMessage}")
    //        Left(WorkItemProcessingException(s"getting total no of requests failed due to ${exception.getMessage}"))
    //      }
    //    }
  }

  def getNoOfFailureByPsaId(psaId: String): Future[Either[Exception, Long]] = {
    collection.countDocuments(
      filter = Filters.and(
        Filters.eq(workItemFields.status, JsString(PermanentlyFailed.name)),
        Filters.eq("item.psaId", psaId)
      )
    ).toFuture().map(Right(_)).recover {
      case exception: Exception => Left(WorkItemProcessingException(
        s"getting no of failed requests failed due to ${exception.getMessage}"))
    }
    //    val selector = Json.obj(workItemFields.status -> JsString(PermanentlyFailed.name), "item.psaId" -> psaId)
    //    collection.count(Some(selector), None, skip = 0, None, ReadConcern.Local).map(Right(_)).recover {
    //      case exception: Exception => Left(WorkItemProcessingException(
    //        s"getting no of failed requests failed due to ${exception.getMessage}"))
    //    }
  }

  def deleteRequest(id: ObjectId): Future[Boolean] = {
    collection.deleteOne(
      filter = Filters.eq(workItemFields.id, id)
    ).toFuture().map(_ => true)
    //    val selector = BSONDocument(workItemFields.id -> id)
    //    collection.delete.one(selector).map(_.ok)
  }

  def deleteAll(psaId: String): Future[Either[Exception, Boolean]] = {
    collection.deleteOne(
      filter = Filters.eq("item.psaId", psaId)
    ).toFuture().recover {
      case exception: Exception => Left(WorkItemProcessingException(s"deleting all requests failed due to ${exception.getMessage}"))
    }.map(_ => Right(true))
    //    val selector = BSONDocument("item.psaId" -> psaId)
    //    collection.delete.one(selector).map(_.ok).map(Right(_)).recover {
    //      case exception: Exception => Left(WorkItemProcessingException(s"deleting all requests failed due to ${exception.getMessage}"))
    //    }
  }

  //  override def pullOutstanding(failedBefore: DateTime, availableBefore: DateTime)(implicit ec: ExecutionContext):
  //  Future[Option[WorkItem[WorkItemRequest]]] = {
  //
  //    def getWorkItem(idList: IdList): Future[Option[WorkItem[WorkItemRequest]]] = {
  //      collection.find(
  //        filter = Filters.eq(workItemFields.id, idList._id)
  //      ).toFuture().map(_.headOption)
  //
  ////      collection.find(
  ////        selector = Json.obj(workItemFields.id -> ReactiveMongoFormats.objectIdWrite.writes(idList._id)),
  ////        projection = Option.empty[JsObject]
  ////      ).one[WorkItem[WorkItemRequest]]
  //    }
  //
  //    val id = findNextItemId(failedBefore, availableBefore)
  //    id.map(_.map(getWorkItem)).flatMap(_.getOrElse(Future.successful(None)))
  //  }
  //
  //  private def setStatusOperation(newStatus: ProcessingStatus, availableAt: Option[DateTime]): JsObject = {
  //    val fields = Json.obj(
  //      workItemFields.status -> newStatus.name,
  //      workItemFields.updatedAt -> now
  //    ) ++ availableAt.map(when => Json.obj(workItemFields.availableAt -> when)).getOrElse(Json.obj())
  //
  //    val ifFailed =
  //      if (newStatus == Failed)
  //        Json.obj("$inc" -> Json.obj(workItemFields.failureCount -> 1))
  //      else Json.obj()
  //
  //    Json.obj("$set" -> fields) ++ ifFailed
  //  }

  //  private def findNextItemId(failedBefore: DateTime, availableBefore: DateTime)(implicit ec: ExecutionContext): Future[Option[IdList]] = {
  //
  //    def findNextItemIdByQuery(query: JsObject)(implicit ec: ExecutionContext): Future[Option[IdList]] =
  //      findAndUpdate(
  //        query = query,
  //        update = setStatusOperation(InProgress, None),
  //        fetchNewObject = true,
  //        fields = Some(Json.obj(workItemFields.id -> 1))
  //      ).map(_.value.map(Json.toJson(_).as[IdList]))
  //
  //    def todoQuery: JsObject =
  //      Json.obj(
  //        workItemFields.status -> JsString(ToDo.name),
  //        workItemFields.availableAt -> Json.obj("$lt" -> availableBefore)
  //      )
  //
  //    def failedQuery: JsObject =
  //      Json.obj("$or" -> Seq(
  //        Json.obj(workItemFields.status -> JsString(Failed.name), workItemFields.updatedAt -> Json.obj("$lt" -> failedBefore),
  //          workItemFields.availableAt -> Json.obj("$lt" -> availableBefore)),
  //        Json.obj(workItemFields.status -> JsString(Failed.name), workItemFields.updatedAt -> Json.obj("$lt" -> failedBefore),
  //          workItemFields.availableAt -> Json.obj("$exists" -> false))
  //      ))
  //
  //
  //    def inProgressQuery: JsObject =
  //      Json.obj(
  //        workItemFields.status -> JsString(InProgress.name),
  //        workItemFields.updatedAt -> Json.obj("$lt" -> now.minus(inProgressRetryAfter))
  //      )
  //
  //    findNextItemIdByQuery(failedQuery).flatMap {
  //      case None => findNextItemIdByQuery(todoQuery).flatMap {
  //        case None => findNextItemIdByQuery(inProgressQuery)
  //        case item => Future.successful(item)
  //      }
  //      case item => Future.successful(item)
  //    }
  //  }

  case class WorkItemProcessingException(message: String) extends Exception(message)
}
