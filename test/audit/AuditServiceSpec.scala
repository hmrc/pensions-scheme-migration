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

package audit

import audit.AuditServiceSpec.mock
import org.apache.pekko.stream.Materializer
import org.scalatest.Inside
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{ApplicationLifecycle, bind}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeHeaders, FakeRequest}
import repositories._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditChannel, AuditConnector, AuditResult, DatastreamMetrics}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends AsyncFlatSpec with Matchers with Inside {

  import AuditServiceSpec._

  "AuditServiceImpl" should "construct and send the correct event" in {

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest()

    val event = TestAuditEvent("test-audit-payload")

    auditService().sendEvent(event)

    val sentEvent = FakeAuditConnector.lastSentEvent

    inside(sentEvent) {
      case event: DataEvent =>
        event.auditSource shouldBe appName
        event.auditType shouldBe "TestAuditEvent"
        event.detail should contain key ("payload")
    }
  }
}

object AuditServiceSpec extends MockitoSugar {

  private val app = new GuiceApplicationBuilder()
    .overrides(
      bind[AuditConnector].toInstance(FakeAuditConnector),
      bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
      bind[ListOfLegacySchemesCacheRepository].toInstance(mock[ListOfLegacySchemesCacheRepository]),
      bind[LockCacheRepository].toInstance(mock[LockCacheRepository]),
      bind[RacDacRequestsQueueEventsLogRepository].toInstance(mock[RacDacRequestsQueueEventsLogRepository]),
      bind[RacDacRequestsQueueRepository].toInstance(mock[RacDacRequestsQueueRepository]),
      bind[SchemeDataCacheRepository].toInstance(mock[SchemeDataCacheRepository])
    )
    .build()

  def fakeRequest(): FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  def fakeHeader(): FakeHeaders = FakeHeaders(Seq.empty)

  def auditService(): AuditService = app.injector.instanceOf[AuditService]

  def appName: String = app.configuration.underlying.getString("appName")

}

//noinspection ScalaDeprecation
object FakeAuditConnector extends AuditConnector {

  private var sentEvent: DataEvent = mock[DataEvent]
  private var sentExtendedDataEvent: ExtendedDataEvent = mock[ExtendedDataEvent]

  override def auditingConfig: AuditingConfig =
    AuditingConfig(
      consumer = None,
      enabled = false,
      auditSource = "test audit source",
      auditSentHeaders = false
    )

  override def sendEvent(event: DataEvent)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    sentEvent = event
    super.sendEvent(event)
  }

  override def sendExplicitAudit(auditType: String, detail: Map[String, String])
                                (implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    sentExtendedDataEvent = ExtendedDataEvent(
      auditSource = auditingConfig.auditSource,
      auditType = auditType,
      eventId = UUID.randomUUID().toString,
      tags = hc.toAuditTags(),
      detail = Json.toJson(detail).as[JsObject]
    )
    super.sendExtendedEvent(sentExtendedDataEvent)
  }

  def lastSentEvent: DataEvent = sentEvent

  def lastSentExtendedDataEvent: ExtendedDataEvent = sentExtendedDataEvent

  def materializer: Materializer = ???

  def lifecycle: ApplicationLifecycle = ???

  override def auditChannel: AuditChannel = ???

  override def datastreamMetrics: DatastreamMetrics = ???
}

case class TestAuditEvent(payload: String) extends AuditEvent {

  override def auditType: String = "TestAuditEvent"

  override def details: Map[String, String] =
    Map("payload" -> payload)

}
