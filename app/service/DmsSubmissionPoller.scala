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

package service

import akka.actor.ActorSystem
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Logger.logger
import service.DmsSubmissionPoller.OnCompleteHandler
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.workitem.{Failed, PermanentlyFailed, Succeeded}

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

@Singleton
class DmsSubmissionPoller @Inject()(
                                     actorSystem: ActorSystem,
                                     dmsSubmissionService: DefaultDmsSubmissionService,
                                     dmsSubmissionPollerContext: DmsSubmissionPollerExecutionContext,
                                     servicesConfig: ServicesConfig,
                                     onCompleteHandler: OnCompleteHandler
                                   )(implicit
                                     executionContext: DmsSubmissionPollerExecutionContext
                                   ) {

  private val jitteredInitialDelay: FiniteDuration = FiniteDuration(
    servicesConfig.getDuration("dms.submission-poller.initial-delay").toMillis, TimeUnit.MILLISECONDS) +
    FiniteDuration(Random.nextInt((servicesConfig.getDuration("dms.submission-poller.jitter-period").toMillis.toInt + 1)).toLong,
    TimeUnit.MILLISECONDS
  )

  private val pollerInterval: FiniteDuration =
    FiniteDuration(servicesConfig.getDuration("dms.submission-poller.interval").toMillis, TimeUnit.MILLISECONDS)

  private val failureCountLimit: Int = servicesConfig.getInt("dms.submission-poller.failure-count-limit")

  val _ = actorSystem.scheduler.schedule(jitteredInitialDelay, pollerInterval)(poller())(dmsSubmissionPollerContext)

  def poller(): Unit = {
    val result = dmsSubmissionService.dequeue.map {
      case Right(Some(workItem)) =>
        if (workItem.failureCount == failureCountLimit) {
          val _ = dmsSubmissionService.setResultStatus(workItem.id, PermanentlyFailed)
          Future.successful(())
        } else {
          dmsSubmissionService.submitToETMP(workItem.item).map {
            case Right(_) =>
              val _ = dmsSubmissionService.setResultStatus(workItem.id, Succeeded)
            case Left(_) =>
              val _ = dmsSubmissionService.setProcessingStatus(workItem.id, Failed)
          }
        }
      case Right(None) =>
        logger.info("DMS Submission poller: no work items")
        Future.successful(())
      case _ =>
        //TODO: what needs to be done in that case - we need to test this
        logger.info("DMS Submission poller: exception while getting item from queue")
        Future.successful(())
    }

    result.onComplete(_ => onCompleteHandler.onComplete())
  }

}

object DmsSubmissionPoller {

  @ImplementedBy(classOf[DefaultOnCompleteHandler])
  trait OnCompleteHandler {
    def onComplete(): Unit
  }

  @Singleton
  class DefaultOnCompleteHandler extends OnCompleteHandler {
    override def onComplete(): Unit = ()
  }

}

