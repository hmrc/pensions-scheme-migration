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

import akka.actor.{ActorSystem, Cancellable}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import models.racDac.WorkItemRequest
import play.api.Logger
import play.api.http.Status.CONFLICT
import play.api.libs.json.JsValue
import service.RacDacBulkSubmissionPoller.OnCompleteHandler
import uk.gov.hmrc.http.HttpErrorFunctions.{is4xx, is5xx}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Failed, PermanentlyFailed}
import uk.gov.hmrc.mongo.workitem.WorkItem
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration._


@Singleton
class RacDacBulkSubmissionPoller @Inject()(
                                            actorSystem: ActorSystem,
                                            racDacBulkSubmissionService: RacDacBulkSubmissionService,
                                            racDacBulkSubmissionPollerContext: RacDacBulkSubmissionPollerExecutionContext,
                                            servicesConfig: ServicesConfig,
                                            onCompleteHandler: OnCompleteHandler
                                   )(implicit
                                     executionContext: RacDacBulkSubmissionPollerExecutionContext
                                   ) {

  private val logger = Logger(classOf[RacDacBulkSubmissionPoller])

  private val initialDelay: FiniteDuration = FiniteDuration(
    servicesConfig.getDuration("racDacWorkItem.submission-poller.initial-delay").toMillis, TimeUnit.MILLISECONDS)

  private val pollerInterval: FiniteDuration =
    FiniteDuration(servicesConfig.getDuration("racDacWorkItem.submission-poller.interval").toMillis, TimeUnit.MILLISECONDS)

  private val failureCountLimit: Int = servicesConfig.getInt("racDacWorkItem.submission-poller.failure-count-limit")



  val `_`: Cancellable = {
    object MyThread extends Runnable {
      def run(): Unit = {
        poller()
      }
    }
    actorSystem.scheduler.scheduleAtFixedRate(initialDelay, pollerInterval)(MyThread)(racDacBulkSubmissionPollerContext)
  }

  def poller(): Unit = {
    val result = racDacBulkSubmissionService.dequeue.map {
      case Right(Some(workItem)) =>
        if (workItem.failureCount == failureCountLimit) {
          val _ = racDacBulkSubmissionService.setResultStatus(workItem.id, PermanentlyFailed)
          Future.successful(())
        } else {
          racDacBulkSubmissionService.submitToETMP(workItem.item).map { response =>
            assignStatus(workItem, response)
          }
        }
      case Right(None) =>
        logger.info("RacDac Submission poller: no work items")
        Future.successful(())
      case _ =>
        logger.info("RacDac Submission poller: exception while getting item from queue")
        Future.successful(())
    }
      result.onComplete(_ => onCompleteHandler.onComplete())
  }

  private def assignStatus(workItem: WorkItem[WorkItemRequest], response: Either[Exception, JsValue]): Unit = {
    response match {
      case Right(_) | Left(UpstreamErrorResponse(_, CONFLICT, _, _)) =>
        val _ = racDacBulkSubmissionService.deleteRequest(workItem.id)
      case Left(UpstreamErrorResponse(_, status, _, _)) if is4xx(status) =>
        val _ = racDacBulkSubmissionService.setProcessingStatus(workItem.id, PermanentlyFailed)
      case Left(UpstreamErrorResponse(_, status, _, _)) if is5xx(status) =>
        val _ = racDacBulkSubmissionService.setProcessingStatus(workItem.id, Failed)
      case _ =>
        val _ = racDacBulkSubmissionService.setProcessingStatus(workItem.id, Failed)
    }
  }
}


object RacDacBulkSubmissionPoller {

  @ImplementedBy(classOf[DefaultOnCompleteHandler])
  trait OnCompleteHandler {
    def onComplete(): Unit
  }

  @Singleton
  class DefaultOnCompleteHandler extends OnCompleteHandler {
    override def onComplete(): Unit = ()
  }

}

