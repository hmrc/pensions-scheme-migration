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

package controllers

import audit.{AuditService, EmailRequestAuditEvent, RacDacBulkMigrationTriggerAuditEvent}
import base.SpecBase
import config.AppConfig
import connector.{EmailConnector, EmailNotSent, EmailSent, MinimalDetailsConnector}
import models.enumeration.JourneyType.RACDAC_BULK_MIG
import models.{IndividualDetails, MinPSA}
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{Eventually, PatienceConfiguration, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsBoolean, JsValue, Json}
import play.api.mvc.BodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.RacDacRequestsQueueEventsLogRepository
import service.RacDacBulkSubmissionService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._
import utils.AuthUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BulkRacDacControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration with Eventually {
  private val actorSystem = ActorSystem.create("testActorSystem")
  private val mockAppConfig = mock[AppConfig]
  private val mockRacDacRequestsQueueEventsLogRepository = mock[RacDacRequestsQueueEventsLogRepository]
  private val mockAuditService = mock[AuditService]
  private val mockRacDacBulkSubmissionService: RacDacBulkSubmissionService = mock[RacDacBulkSubmissionService]
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockEmailConnector: EmailConnector = mock[EmailConnector]
  private val mockMinimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]
  private val baseUrl = "/dummy-base-url"
  private val sessionId = AuthUtils.id
  private val emailTemplate = "dummyTemplate"
  private val psaId = "A2000001"
  private val minPsa = MinPSA(
    email = "a@a.c",
    isPsaSuspended = false,
    organisationName = None,
    individualDetails = Some(IndividualDetails("Bill", None, "Bloggs")),
    rlsFlag = false,
    deceasedFlag = false)

  private val bulkRacDacController = new BulkRacDacController(
    mockAppConfig,
    stubControllerComponents(),
    mockRacDacBulkSubmissionService,
    mockAuditService,
    mockRacDacRequestsQueueEventsLogRepository,
    actorSystem,
    mockEmailConnector,
    mockMinimalDetailsConnector,
    crypto,
    new actions.AuthAction(mockAuthConnector, app.injector.instanceOf[BodyParsers.Default])
  )

  before {
    reset(
      mockAppConfig,
      mockRacDacBulkSubmissionService, mockAuditService, mockAuthConnector,
      mockRacDacRequestsQueueEventsLogRepository, mockEmailConnector, mockMinimalDetailsConnector
    )
    when(mockAppConfig.bulkMigrationConfirmationEmailTemplateId).thenReturn(emailTemplate)
    when(mockAppConfig.baseUrlPensionsSchemeMigration).thenReturn(baseUrl)
    AuthUtils.authStub(mockAuthConnector)
    when(mockRacDacRequestsQueueEventsLogRepository.save(any(), any())(any()))
      .thenReturn(Future.successful(true))
    when(mockRacDacRequestsQueueEventsLogRepository.remove(any())(any()))
      .thenReturn(Future.successful(true))
    when(mockEmailConnector.sendEmail(any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(EmailSent))
    when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any()))
      .thenReturn(Future.successful(Right(minPsa)))
  }

  "migrateAllRacDac" must {
    val jsValue =
      """[{"schemeName":"paul qqq","policyNumber":"24101975","pstr":"00615269RH"
        ,"declarationDate":"2012-02-20","schemeOpenDate":"2020-01-01"}]""".stripMargin
    val fakeRequest = FakeRequest("POST", "/").withHeaders(("psaId", psaId),
      HeaderNames.xSessionId -> sessionId).withJsonBody(Json.parse(jsValue))

    "return ACCEPTED, send audit event, email and email audit event if all the rac dac requests are successfully pushed to the queue" in {
      val captor = ArgumentCaptor.forClass(classOf[RacDacBulkMigrationTriggerAuditEvent])
      doNothing().when(mockAuditService).sendEvent(captor.capture())(any(), any())
      when(mockRacDacBulkSubmissionService.enqueue(any())).thenReturn(Future.successful(true))

      val result = bulkRacDacController.clearEventLogThenInitiateMigration(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        verify(mockRacDacRequestsQueueEventsLogRepository, times(1)).remove(any())(any())

        eventually {
          verify(mockRacDacBulkSubmissionService, times(1)).enqueue(any())
          verify(mockAuditService, times(1))
            .sendEvent(ArgumentMatchers.eq(RacDacBulkMigrationTriggerAuditEvent("A2000001", 1, "")))(any(), any())
          verify(mockMinimalDetailsConnector, times(1)).getPSADetails(ArgumentMatchers.eq(psaId))(any(), any())
          verify(mockEmailConnector, times(1))
            .sendEmail(
              emailAddress = ArgumentMatchers.eq(minPsa.email),
              templateName = ArgumentMatchers.eq(emailTemplate),
              params = ArgumentMatchers.eq(Map("psaName" -> minPsa.name)),
              callbackUrl = ArgumentMatchers.startsWith(
                s"$baseUrl/pensions-scheme-migration/email-response/RetirementOrDeferredAnnuityContractBulkMigration/"
              )
            )(any(), any())
          verify(mockAuditService, times(1))
            .sendEvent(ArgumentMatchers.eq(EmailRequestAuditEvent(psaId, RACDAC_BULK_MIG, minPsa.email, pstrId = "")))(any(), any())
          val jsonCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
          verify(mockRacDacRequestsQueueEventsLogRepository, times(1)).save(ArgumentMatchers.eq(sessionId), jsonCaptor.capture())(any())
          jsonCaptor.getValue mustBe Json.obj("status" -> ACCEPTED)
        }
      }
    }

    "return ACCEPTED, send audit event but no email audit event if all the rac dac requests are successfully pushed to the " +
      "queue but min details call fails" in {
      val captor = ArgumentCaptor.forClass(classOf[RacDacBulkMigrationTriggerAuditEvent])
      doNothing().when(mockAuditService).sendEvent(captor.capture())(any(), any())
      when(mockRacDacBulkSubmissionService.enqueue(any())).thenReturn(Future.successful(true))

      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any()))
        .thenReturn(Future.successful(Left(new HttpException("test", INTERNAL_SERVER_ERROR))))

      val result = bulkRacDacController.clearEventLogThenInitiateMigration(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        verify(mockRacDacRequestsQueueEventsLogRepository, times(1)).remove(any())(any())

        eventually {
          verify(mockRacDacBulkSubmissionService, times(1)).enqueue(any())
          verify(mockAuditService, times(1))
            .sendEvent(ArgumentMatchers.eq(RacDacBulkMigrationTriggerAuditEvent("A2000001", 1, "")))(any(), any())
          verify(mockMinimalDetailsConnector, times(1)).getPSADetails(ArgumentMatchers.eq(psaId))(any(), any())
          verify(mockEmailConnector, times(0))
            .sendEmail(
              emailAddress = ArgumentMatchers.eq(minPsa.email),
              templateName = ArgumentMatchers.eq(emailTemplate),
              params = ArgumentMatchers.eq(Map("psaName" -> minPsa.name)),
              callbackUrl = ArgumentMatchers.startsWith(
                s"$baseUrl/pensions-scheme-migration/email-response/RetirementOrDeferredAnnuityContractBulkMigration/"
              )
            )(any(), any())
          verify(mockAuditService, times(0))
            .sendEvent(ArgumentMatchers.eq(EmailRequestAuditEvent(psaId, RACDAC_BULK_MIG, minPsa.email, pstrId = "")))(any(), any())
          val jsonCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
          verify(mockRacDacRequestsQueueEventsLogRepository, times(1)).save(ArgumentMatchers.eq(sessionId), jsonCaptor.capture())(any())
          jsonCaptor.getValue mustBe Json.obj("status" -> ACCEPTED)
        }
      }
    }

    "return ACCEPTED, send audit event but no email audit event if all the rac dac requests are successfully pushed to the " +
      "queue but email not sent" in {
      val captor = ArgumentCaptor.forClass(classOf[RacDacBulkMigrationTriggerAuditEvent])
      doNothing().when(mockAuditService).sendEvent(captor.capture())(any(), any())
      when(mockRacDacBulkSubmissionService.enqueue(any())).thenReturn(Future.successful(true))

      when(mockEmailConnector.sendEmail(any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(EmailNotSent))

      val result = bulkRacDacController.clearEventLogThenInitiateMigration(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        verify(mockRacDacRequestsQueueEventsLogRepository, times(1)).remove(any())(any())

        eventually {
          verify(mockRacDacBulkSubmissionService, times(1)).enqueue(any())
          verify(mockAuditService, times(1))
            .sendEvent(ArgumentMatchers.eq(RacDacBulkMigrationTriggerAuditEvent("A2000001", 1, "")))(any(), any())
          verify(mockMinimalDetailsConnector, times(1)).getPSADetails(ArgumentMatchers.eq(psaId))(any(), any())
          verify(mockEmailConnector, times(1))
            .sendEmail(
              emailAddress = ArgumentMatchers.eq(minPsa.email),
              templateName = ArgumentMatchers.eq(emailTemplate),
              params = ArgumentMatchers.eq(Map("psaName" -> minPsa.name)),
              callbackUrl = ArgumentMatchers.startsWith(
                s"$baseUrl/pensions-scheme-migration/email-response/RetirementOrDeferredAnnuityContractBulkMigration/"
              )
            )(any(), any())
          verify(mockAuditService, times(0))
            .sendEvent(ArgumentMatchers.eq(EmailRequestAuditEvent(psaId, RACDAC_BULK_MIG, minPsa.email, pstrId = "")))(any(), any())
          val jsonCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
          verify(mockRacDacRequestsQueueEventsLogRepository, times(1)).save(ArgumentMatchers.eq(sessionId), jsonCaptor.capture())(any())
          jsonCaptor.getValue mustBe Json.obj("status" -> ACCEPTED)
        }
      }
    }

    "return ServiceUnavailable and send audit event if NOT all the rac dac requests are successfully pushed to the queue" in {
      val captor = ArgumentCaptor.forClass(classOf[RacDacBulkMigrationTriggerAuditEvent])
      doNothing().when(mockAuditService).sendEvent(captor.capture())(any(), any())
      when(mockRacDacBulkSubmissionService.enqueue(any())).thenReturn(Future.successful(false))

      val result = bulkRacDacController.clearEventLogThenInitiateMigration(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        verify(mockRacDacRequestsQueueEventsLogRepository, times(1)).remove(any())(any())

        eventually {
          verify(mockRacDacBulkSubmissionService, times(1)).enqueue(any())
          verify(mockAuditService, times(1))
            .sendEvent(ArgumentMatchers.eq(RacDacBulkMigrationTriggerAuditEvent("A2000001", 0, "Queue Service Unavailable")))(any(), any())
          verify(mockMinimalDetailsConnector, times(0)).getPSADetails(ArgumentMatchers.eq(psaId))(any(), any())
          verify(mockEmailConnector, times(0))
            .sendEmail(
              emailAddress = ArgumentMatchers.eq(minPsa.email),
              templateName = ArgumentMatchers.eq(emailTemplate),
              params = ArgumentMatchers.eq(Map("psaName" -> minPsa.name)),
              callbackUrl = ArgumentMatchers.startsWith(
                s"$baseUrl/pensions-scheme-migration/email-response/RetirementOrDeferredAnnuityContractBulkMigration/"
              )
            )(any(), any())
          verify(mockAuditService, times(0))
            .sendEvent(ArgumentMatchers.eq(EmailRequestAuditEvent(psaId, RACDAC_BULK_MIG, minPsa.email, pstrId = "")))(any(), any())
          val jsonCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
          verify(mockRacDacRequestsQueueEventsLogRepository, times(1)).save(ArgumentMatchers.eq(sessionId), jsonCaptor.capture())(any())
          jsonCaptor.getValue mustBe Json.obj("status" -> SERVICE_UNAVAILABLE)
        }
      }
    }


    "throw BadRequestException and send audit event when request is not valid" in {
      val fakeRequest = FakeRequest("POST", "/").withHeaders(
        "psaId" -> psaId,
        HeaderNames.xSessionId -> sessionId
      ).withJsonBody(Json.obj("invalid" -> "request"))
      val captor = ArgumentCaptor.forClass(classOf[RacDacBulkMigrationTriggerAuditEvent])
      doNothing().when(mockAuditService).sendEvent(captor.capture())(any(), any())
      when(mockRacDacBulkSubmissionService.enqueue(any())).thenReturn(Future.successful(false))

      val result = bulkRacDacController.clearEventLogThenInitiateMigration(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Invalid request received from frontend for rac dac migration"
        verify(mockRacDacRequestsQueueEventsLogRepository, times(0)).remove(any())(any())

        verify(mockRacDacBulkSubmissionService, times(0)).enqueue(any())
        verify(mockAuditService, times(1))
          .sendEvent(ArgumentMatchers.eq
          (RacDacBulkMigrationTriggerAuditEvent(psaId, 0, "Invalid request received from frontend for rac dac migration")))(any(), any())
        verify(mockMinimalDetailsConnector, times(0)).getPSADetails(ArgumentMatchers.eq(psaId))(any(), any())
        verify(mockEmailConnector, times(0))
          .sendEmail(
            emailAddress = ArgumentMatchers.eq(minPsa.email),
            templateName = ArgumentMatchers.eq(emailTemplate),
            params = ArgumentMatchers.eq(Map("psaName" -> minPsa.name)),
            callbackUrl = ArgumentMatchers.startsWith(
              s"$baseUrl/pensions-scheme-migration/email-response/RetirementOrDeferredAnnuityContractBulkMigration/"
            )
          )(any(), any())
        verify(mockAuditService, times(0))
          .sendEvent(ArgumentMatchers.eq(EmailRequestAuditEvent(psaId, RACDAC_BULK_MIG, minPsa.email, pstrId = "")))(any(), any())
        val jsonCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
        verify(mockRacDacRequestsQueueEventsLogRepository, times(0)).save(ArgumentMatchers.eq(sessionId), jsonCaptor.capture())(any())
      }
    }
  }

  "isRequestSubmitted" must {
    val fakeRequest = FakeRequest("GET", "/").withHeaders(("psaId", "A2000001"))

    "return OK with true if some request is in the queue" in {
      when(mockRacDacBulkSubmissionService.isRequestSubmitted(any())).thenReturn(Future.successful(Right(true)))
      val result = bulkRacDacController.isRequestSubmitted(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsBoolean(true)
        verify(mockRacDacBulkSubmissionService, times(1)).isRequestSubmitted(any())
      }
    }

    "return OK with false if no request is in the queue" in {
      when(mockRacDacBulkSubmissionService.isRequestSubmitted(any())).thenReturn(Future.successful(Right(false)))
      val result = bulkRacDacController.isRequestSubmitted(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsBoolean(false)
        verify(mockRacDacBulkSubmissionService, times(1)).isRequestSubmitted(any())
      }
    }


    "return SERVICE UNAVAILABLE if there is an error occurred while querying" in {
      when(mockRacDacBulkSubmissionService.isRequestSubmitted(any())).thenReturn(Future.successful(Left(new Exception("message"))))
      val result = bulkRacDacController.isRequestSubmitted(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe SERVICE_UNAVAILABLE
        verify(mockRacDacBulkSubmissionService, times(1)).isRequestSubmitted(any())
      }
    }
  }

  "isAllFailed" must {
    val fakeRequest = FakeRequest("GET", "/").withHeaders(("psaId", "A2000001"))

    "return No Content if there is no requests in the queue" in {
      when(mockRacDacBulkSubmissionService.isAllFailed(any())).thenReturn(Future.successful(Right(None)))
      val result = bulkRacDacController.isAllFailed(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe NO_CONTENT
        verify(mockRacDacBulkSubmissionService, times(1)).isAllFailed(any())
      }
    }

    "return OK with true if all requests in the queue is failed" in {
      when(mockRacDacBulkSubmissionService.isAllFailed(any())).thenReturn(Future.successful(Right(Some(true))))
      val result = bulkRacDacController.isAllFailed(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsBoolean(true)
        verify(mockRacDacBulkSubmissionService, times(1)).isAllFailed(any())
      }
    }

    "return OK with false if some requests in the queue is not failed" in {
      when(mockRacDacBulkSubmissionService.isAllFailed(any())).thenReturn(Future.successful(Right(Some(false))))
      val result = bulkRacDacController.isAllFailed(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsBoolean(false)
        verify(mockRacDacBulkSubmissionService, times(1)).isAllFailed(any())
      }
    }

    "return SERVICE_UNAVAILABLE if there is an error occurred" in {
      when(mockRacDacBulkSubmissionService.isAllFailed(any())).thenReturn(Future.successful(Left(new Exception("message"))))
      val result = bulkRacDacController.isAllFailed(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe SERVICE_UNAVAILABLE
        verify(mockRacDacBulkSubmissionService, times(1)).isAllFailed(any())
      }
    }
  }

  "deleteAll" must {
    val fakeRequest = FakeRequest("DELETE", "/").withHeaders(("psaId", "A2000001"))

    "return OK with true if all requests in the queue is deleted" in {
      when(mockRacDacBulkSubmissionService.deleteAll(any())).thenReturn(Future.successful(Right(true)))
      val result = bulkRacDacController.deleteAll(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsBoolean(true)
        verify(mockRacDacBulkSubmissionService, times(1)).deleteAll(any())
      }
    }


    "return SERVICE_UNAVAILABLE if there is an error occurred" in {
      when(mockRacDacBulkSubmissionService.deleteAll(any())).thenReturn(Future.successful(Left(new Exception("message"))))
      val result = bulkRacDacController.deleteAll(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe SERVICE_UNAVAILABLE
        verify(mockRacDacBulkSubmissionService, times(1)).deleteAll(any())
      }
    }
  }
}

