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

import models.cache.MigrationLock
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.AuthUtils

import scala.concurrent.Future

class LockCacheControllerSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfter {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val repo = mock[LockCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val id = AuthUtils.id
  private val pstr = "pstr"
  private val psaId = AuthUtils.psaId
  private val lock: MigrationLock = MigrationLock(pstr, id, psaId)
  private val fakeRequest = FakeRequest().withHeaders("pstr" -> pstr)
  private val fakePostRequest = FakeRequest("POST", "/").withHeaders("pstr" -> pstr)

  private val modules: Seq[GuiceableModule] = Seq(
    bind[AuthConnector].toInstance(authConnector),
    bind[LockCacheRepository].toInstance(repo),
    bind[AdminDataRepository].toInstance(mock[AdminDataRepository]),
    bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
    bind[ListOfLegacySchemesCacheRepository].toInstance(mock[ListOfLegacySchemesCacheRepository]),
    bind[RacDacRequestsQueueRepository].toInstance(mock[RacDacRequestsQueueRepository]),
    bind[SchemeDataCacheRepository].toInstance(mock[SchemeDataCacheRepository]),
    bind[RacDacRequestsQueueEventsLogRepository].toInstance(mock[RacDacRequestsQueueEventsLogRepository])
  )

  private val app = new GuiceApplicationBuilder()
    .configure(
      conf = "auditing.enabled" -> false,
      "metrics.enabled" -> false,
      "metrics.jvm" -> false,
      "run.mode" -> "Test"
    ).overrides(modules: _*).build()

  private val controller = app.injector.instanceOf[LockCacheController]

  before {
    reset(repo, authConnector)
  }

  "LockCacheController" when {
    "calling getLockOnScheme" must {
      "return OK with the data" in {
        when(repo.getLockByPstr(eqTo(pstr))) thenReturn Future.successful(Some(lock))
        AuthUtils.authStub(authConnector)

        val result = controller.getLockOnScheme(fakeRequest)
        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(lock)
      }

      "return NOT FOUND when the data doesn't exist" in {
        when(repo.getLockByPstr(eqTo(pstr))) thenReturn Future.successful(None)
        AuthUtils.authStub(authConnector)

        val result = controller.getLockOnScheme(fakeRequest)
        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.getLockByPstr(eqTo(pstr))) thenReturn Future.failed(new Exception())
        AuthUtils.authStub(authConnector)

        val result = controller.getLockOnScheme(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }

      "throw an exception when the call is not authorised" in {
        AuthUtils.failedAuthStub(authConnector)

        val result = controller.getLockOnScheme(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }

      "return a BadRequestException when no pstr in headers" in {
        when(repo.getLockByPstr(eqTo(pstr))) thenReturn Future.successful(Some(lock))
        AuthUtils.authStub(authConnector)

        val result = controller.getLockOnScheme(FakeRequest())
        ScalaFutures.whenReady(result.failed) { res =>
          res mustBe a[BadRequestException]
          res.getMessage mustBe "Bad Request without pstr"
        }
      }

    }

    "calling getLock" must {
      "return OK with the data" in {
        when(repo.getLock(eqTo(lock))(any())) thenReturn Future.successful(Some(lock))
        AuthUtils.authStub(authConnector)

        val result = controller.getLock(fakeRequest)
        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(lock)
      }

      "return NOT FOUND when the data doesn't exist" in {
        when(repo.getLock(eqTo(lock))(any())) thenReturn Future.successful(None)
        AuthUtils.authStub(authConnector)

        val result = controller.getLock(fakeRequest)
        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.getLock(eqTo(lock))(any())) thenReturn Future.failed(new Exception())
        AuthUtils.authStub(authConnector)

        val result = controller.getLock(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }

    }

    "calling getLockByUser" must {
      "return OK with the data" in {
        when(repo.getLockByCredId(eqTo(id))) thenReturn Future.successful(Some(lock))
        AuthUtils.authStub(authConnector)

        val result = controller.getLockByUser(fakeRequest)
        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(lock)
      }

      "return NOT FOUND when the data doesn't exist" in {
        when(repo.getLockByCredId(eqTo(id))) thenReturn Future.successful(None)
        AuthUtils.authStub(authConnector)

        val result = controller.getLockByUser(fakeRequest)
        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.getLockByCredId(eqTo(id))) thenReturn Future.failed(new Exception())
        AuthUtils.authStub(authConnector)

        val result = controller.getLockByUser(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }

    }

    "calling save" must {

      "return OK when the data is saved successfully" in {
        when(repo.setLock(any())) thenReturn Future.successful(true)
        AuthUtils.authStub(authConnector)

        val result = controller.lock(fakePostRequest)
        status(result) mustEqual OK
      }
      "return Conflict when the data is not saved successfully" in {
        when(repo.setLock(any())) thenReturn Future.successful(false)
        AuthUtils.authStub(authConnector)

        val result = controller.lock(fakePostRequest)
        status(result) mustEqual CONFLICT
      }
    }

    "calling removeLockOnScheme" must {
      "return OK when the data is removed successfully" in {
        when(repo.releaseLockByPstr(eqTo(pstr))) thenReturn Future.successful(true)
        AuthUtils.authStub(authConnector)

        val result = controller.removeLockOnScheme()(fakeRequest)
        status(result) mustEqual OK
      }

      "throw an exception when the call is not authorised" in {
        AuthUtils.failedAuthStub(authConnector)

        val result = controller.removeLockOnScheme()(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }

      "return a BadRequestException when no pstr in headers" in {
        when(repo.releaseLockByPstr(eqTo(pstr))) thenReturn Future.successful(true)
        AuthUtils.authStub(authConnector)

        val result = controller.removeLockOnScheme()(FakeRequest())

        ScalaFutures.whenReady(result.failed) { res =>
          res mustBe a[BadRequestException]
          res.getMessage mustBe "Bad Request without pstr"
        }
      }
    }

    "calling removeLockByUser" must {
      "return OK when the data is removed successfully" in {
        when(repo.releaseLockByCredId(eqTo(id))) thenReturn Future.successful(true)
        AuthUtils.authStub(authConnector)

        val result = controller.removeLockByUser()(fakeRequest)
        status(result) mustEqual OK
      }
    }

    "calling removeLock" must {
      "return OK when the data is removed successfully" in {
        when(repo.releaseLock(eqTo(lock))) thenReturn Future.successful(true)
        AuthUtils.authStub(authConnector)

        val result = controller.removeLock()(fakeRequest)
        status(result) mustEqual OK
      }
    }
  }
}
