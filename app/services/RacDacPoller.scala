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

package uk.gov.hmrc.cgtpropertydisposals.service.dms

import java.util.concurrent.TimeUnit
import akka.actor.ActorSystem
import cats.data.EitherT
import cats.instances.future._
import cats.instances.int._
import cats.syntax.eq._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.cgtpropertydisposals.models.{Error, UUIDGenerator}
import uk.gov.hmrc.cgtpropertydisposals.service.DmsSubmissionService
import uk.gov.hmrc.cgtpropertydisposals.service.dms.DmsSubmissionPoller.OnCompleteHandler
import uk.gov.hmrc.cgtpropertydisposals.util.Logging
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.workitem.{Failed, PermanentlyFailed, Succeeded, WorkItem}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

@Singleton
class RacDacPoller @Inject() (
                                      actorSystem: ActorSystem,
                                      dmsSubmissionService: DmsSubmissionService,
                                      dmsSubmissionPollerContext: DmsSubmissionPollerExecutionContext,
                                      servicesConfig: ServicesConfig,
                                      onCompleteHandler: OnCompleteHandler,
                                      uuidGenerator: UUIDGenerator
                                    )(implicit
                                      executionContext: DmsSubmissionPollerExecutionContext
                                    ) {

  val logger = Logger(classOf[RacDacPoller])
  private val jitteredInitialDelay: FiniteDuration = FiniteDuration(
    servicesConfig.getDuration("dms.submission-poller.initial-delay").toMillis,
    TimeUnit.MILLISECONDS
  ) + FiniteDuration(
    Random.nextInt((servicesConfig.getDuration("dms.submission-poller.jitter-period").toMillis.toInt + 1)).toLong,
    TimeUnit.MILLISECONDS
  )

  private val formIdConst = "CGTSUBMITDOC"

  private val pollerInterval: FiniteDuration =
    FiniteDuration(servicesConfig.getDuration("dms.submission-poller.interval").toMillis, TimeUnit.MILLISECONDS)

  private val failureCountLimit: Int = servicesConfig.getInt("dms.submission-poller.failure-count-limit")

  val _ = actorSystem.scheduler.schedule(jitteredInitialDelay, pollerInterval)(poller())(dmsSubmissionPollerContext)

  def getLogMessage(workItem: WorkItem[DmsSubmissionRequest], stateIndicator: String): String =
    s"DMS Submission poller: $stateIndicator:  work-item-id: ${workItem.id}, work-item-failure-count: ${workItem.failureCount}, " +
      s"work-item-status: ${workItem.status}, work-item-updatedAt : ${workItem.updatedAt}, " +
      s"work-item-cgt-reference: ${workItem.item.cgtReference}, " +
      s"work-item-form-bundle-id: ${workItem.item.formBundleId}"

  def poller(): Unit = {
    val result: EitherT[Future, Error, Unit] = dmsSubmissionService.dequeue.semiflatMap {
      case Some(workItem) =>
        if (workItem.failureCount === failureCountLimit) {
          logger.warn(getLogMessage(workItem, "work-item permanently failed"))
          val _ = dmsSubmissionService.setResultStatus(workItem.id, PermanentlyFailed)
          Future.successful(())
        } else {
          val id = uuidGenerator.nextId()
          logger.info(getLogMessage(workItem, s"processing work-item with id $id"))

          dmsSubmissionService
            .submitToDms(
              workItem.item.html,
              formIdConst,
              workItem.item.cgtReference,
              workItem.item.completeReturn,
              id
            )
            .fold(
              { error =>
                logger.warn(getLogMessage(workItem, s"work-item failed with error: $error"))
                val _ = dmsSubmissionService.setProcessingStatus(workItem.id, Failed)
              },
              { envelopeId =>
                logger.info(getLogMessage(workItem, s"work-item succeeded: envelope id : $envelopeId"))
                val _ = dmsSubmissionService.setResultStatus(workItem.id, Succeeded)
              }
            )
        }

      case None =>
        logger.info("DMS Submission poller: no work items")
        Future.successful(())
    }

    result.value.onComplete(_ => onCompleteHandler.onComplete())
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
