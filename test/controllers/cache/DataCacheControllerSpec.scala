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

import org.apache.commons.lang3.RandomUtils
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfter
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
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthUtils

import scala.concurrent.Future

class DataCacheControllerSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfter { // scalastyle:off magic.number

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val repo = mock[DataCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val pstr = "pstr"
  private val psaId = AuthUtils.psaId
  private val fakeRequest = FakeRequest().withHeaders("pstr" -> pstr, "psaId" -> psaId)
  private val fakePostRequest = FakeRequest("POST", "/").withHeaders("pstr" -> pstr, "psaId" -> psaId)

  val app: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
    .overrides(
      bind[AuthConnector].toInstance(authConnector),
      bind[DataCacheRepository].toInstance(repo),
      bind[AdminDataRepository].toInstance(mock[AdminDataRepository]),
      bind[ListOfLegacySchemesCacheRepository].toInstance(mock[ListOfLegacySchemesCacheRepository]),
      bind[LockCacheRepository].toInstance(mock[LockCacheRepository]),
      bind[RacDacRequestsQueueEventsLogRepository].toInstance(mock[RacDacRequestsQueueEventsLogRepository]),
      bind[RacDacRequestsQueueRepository].toInstance(mock[RacDacRequestsQueueRepository]),
      bind[SchemeDataCacheRepository].toInstance(mock[SchemeDataCacheRepository])
    ).build()

  before {
    reset(repo)
    reset(authConnector)
  }

  "DataCacheController" when {
    "calling get" must {
      "return OK with the data" in {
        val controller = app.injector.instanceOf[DataCacheController]
        when(repo.get(eqTo(pstr))(any())) thenReturn Future.successful(Some(Json.obj("testId" -> "data")))
        AuthUtils.authStub(authConnector)

        val result = controller.get(fakeRequest)
        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.obj(fields = "testId" -> "data")
      }

      "return NOT FOUND when the data doesn't exist" in {
        val controller = app.injector.instanceOf[DataCacheController]
        when(repo.get(eqTo(pstr))(any())) thenReturn Future.successful(None)
        AuthUtils.authStub(authConnector)

        val result = controller.get(fakeRequest)
        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        val controller = app.injector.instanceOf[DataCacheController]
        when(repo.get(eqTo(pstr))(any())) thenReturn Future.failed(new Exception())
        AuthUtils.authStub(authConnector)

        val result = controller.get(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }

    }

    "calling save" must {

      "return OK when the data is saved successfully" in {
        val controller = app.injector.instanceOf[DataCacheController]
        when(repo.renewLockAndSave(any(), any())(any())) thenReturn Future.successful(true)
        AuthUtils.authStub(authConnector)

        val result = controller.save(fakePostRequest.withJsonBody(Json.obj("value" -> "data")))
        status(result) mustEqual OK
      }

      "return BAD REQUEST when the request body cannot be parsed" in {
        val controller = app.injector.instanceOf[DataCacheController]
        when(repo.renewLockAndSave(any(), any())(any())) thenReturn Future.successful(true)
        AuthUtils.authStub(authConnector)

        val result = controller.save(fakePostRequest.withRawBody(ByteString(RandomUtils.nextBytes(512001))))
        status(result) mustEqual BAD_REQUEST
      }
    }

    "calling remove" must {
      "return OK when the data is removed successfully" in {
        val controller = app.injector.instanceOf[DataCacheController]
        when(repo.remove(eqTo(pstr))(any())) thenReturn Future.successful(true)
        AuthUtils.authStub(authConnector)

        val result = controller.remove(fakeRequest)
        status(result) mustEqual OK
      }
    }
  }
}

