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

package controllers.cache

import models.racDac.SessionIdNotFound
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import utils.AuthUtils

import scala.concurrent.Future

class RacDacRequestsQueueEventsLogControllerSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfter {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val repo: RacDacRequestsQueueEventsLogRepository = mock[RacDacRequestsQueueEventsLogRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val sessionId = "123"
  private val fakeRequest = FakeRequest().withHeaders(HeaderNames.xSessionId -> "123")

  private val app: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
    .overrides(bind[AuthConnector].toInstance(authConnector),
      bind[RacDacRequestsQueueEventsLogRepository].toInstance(repo),
      bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
      bind[ListOfLegacySchemesCacheRepository].toInstance(mock[ListOfLegacySchemesCacheRepository]),
      bind[LockCacheRepository].toInstance(mock[LockCacheRepository]),
      bind[RacDacRequestsQueueRepository].toInstance(mock[RacDacRequestsQueueRepository]),
      bind[SchemeDataCacheRepository].toInstance(mock[SchemeDataCacheRepository]))
    .build()

  private val controller: RacDacRequestsQueueEventsLogController = app.injector.instanceOf[RacDacRequestsQueueEventsLogController]

  before {
    reset(repo)
    reset(authConnector)
    AuthUtils.authStub(authConnector)
  }

  "RacDacRequestsQueueEventsLogController" when {
    "calling getStatus" must {
      "return OK when the status is OK" in {
        when(repo.get(eqTo(sessionId))(any())) thenReturn Future.successful(Some(Json.obj("status" -> OK)))

        val result = controller.getStatus(fakeRequest)
        status(result) mustEqual OK
      }

      "return 500 when the status is 500" in {
        when(repo.get(eqTo(sessionId))(any())) thenReturn Future.successful(Some(Json.obj("status" -> INTERNAL_SERVER_ERROR)))

        val result = controller.getStatus(fakeRequest)
        status(result) mustEqual INTERNAL_SERVER_ERROR
      }

      "return NOT FOUND when not present in repository" in {
        when(repo.get(eqTo(sessionId))(any())) thenReturn Future.successful(None)

        val result = controller.getStatus(fakeRequest)
        status(result) mustEqual NOT_FOUND
      }

      "return NOT FOUND when status is not found in returned json" in {
        when(repo.get(eqTo(sessionId))(any())) thenReturn Future.successful(Some(Json.obj()))

        val result = controller.getStatus(fakeRequest)
        status(result) mustEqual NOT_FOUND
      }

      "return SessionIdNotFound sessionId not present" in {
        val fakeRequest = FakeRequest()
        val result = controller.getStatus(fakeRequest)
        ScalaFutures.whenReady(result.failed) { res =>
          res mustBe a[SessionIdNotFound]
        }
      }
    }

  }
}
