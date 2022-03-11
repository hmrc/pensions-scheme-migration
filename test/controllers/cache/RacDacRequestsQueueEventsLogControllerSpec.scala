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

package controllers.cache

import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.RacDacRequestsQueueEventsLogRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}

import scala.concurrent.Future

class RacDacRequestsQueueEventsLogControllerSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfter {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val repo = mock[RacDacRequestsQueueEventsLogRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val id = "id"
  private val fakeRequest = FakeRequest().withHeaders(HeaderNames.xSessionId -> "123")

  private val modules: Seq[GuiceableModule] = Seq(
    bind[AuthConnector].toInstance(authConnector),
    bind[RacDacRequestsQueueEventsLogRepository].toInstance(repo)
  )

  before {
    reset(repo)
    reset(authConnector)
  }

  "RacDacRequestsQueueEventsLogController" when {
    "calling getStatus" must {
      "return OK when the status is OK" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()

        val controller = app.injector.instanceOf[RacDacRequestsQueueEventsLogController]

        when(repo.get(eqTo("123"))(any())) thenReturn Future.successful(Some(Json.obj("status" -> OK)))
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.getStatus(fakeRequest)
        status(result) mustEqual OK
      }

      "return 500 when the status is 500" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()

        val controller = app.injector.instanceOf[RacDacRequestsQueueEventsLogController]

        when(repo.get(eqTo("123"))(any())) thenReturn Future.successful(Some(Json.obj("status" -> INTERNAL_SERVER_ERROR)))
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.getStatus(fakeRequest)
        status(result) mustEqual INTERNAL_SERVER_ERROR
      }

      "return NOT FOUND when not present in repository" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()

        val controller = app.injector.instanceOf[RacDacRequestsQueueEventsLogController]

        when(repo.get(eqTo("123"))(any())) thenReturn Future.successful(None)
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.getStatus(fakeRequest)
        status(result) mustEqual NOT_FOUND
      }
    }

  }
}