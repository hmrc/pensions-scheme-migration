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

import base.SpecBase
import models.FeatureToggle.Enabled
import models.FeatureToggleName.DummyToggle
import models.ToggleDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsBoolean, Json}
import play.api.test.Helpers._
import repositories.{AdminDataRepository, ToggleDataRepository}
import services.FeatureToggleService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FeatureToggleControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockAdminDataRepository = mock[AdminDataRepository]
  private val mockToggleDataRepository = mock[ToggleDataRepository]
  private val mockFeatureToggleService = mock[FeatureToggleService]
  private val toggleDetails = ToggleDetails("Test toggle", Some("Test Desc"), isEnabled = true)

  override def beforeEach(): Unit = {
    reset(mockAdminDataRepository)
    reset(mockFeatureToggleService)
    when(mockAdminDataRepository.getFeatureToggles)
      .thenReturn(Future.successful(Seq(Enabled(DummyToggle))))
    when(mockFeatureToggleService.getAll)
      .thenReturn(Future.successful(Seq(Enabled(DummyToggle))))
    when(mockFeatureToggleService.getAllFeatureToggles)
      .thenReturn(Future.successful(Seq[ToggleDetails](toggleDetails)))
  }

  "FeatureToggleController.getAll" must {
    "return OK and the feature toggles when they exist" in {

      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.getAll()(fakeRequest)

      status(result) mustBe OK
    }
  }

  "FeatureToggleController.get" must {
    "get the feature toggle value and return OK" in {
      when(mockAdminDataRepository.setFeatureToggles(any()))
        .thenReturn(Future.successful(()))

      when(mockFeatureToggleService.get(any()))
        .thenReturn(Future.successful(Enabled(DummyToggle)))

      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.get(DummyToggle)(fakeRequest)

      status(result) mustBe OK

      verify(mockFeatureToggleService, times(1))
        .get(name = DummyToggle)
    }
  }

  "FeatureToggleController.put" must {
    "set the feature toggles and return NO_CONTENT" in {
      when(mockAdminDataRepository.setFeatureToggles(any()))
        .thenReturn(Future.successful(()))

      when(mockFeatureToggleService.set(any(), any()))
        .thenReturn(Future.successful(()))

      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.put(DummyToggle)(fakeRequest.withJsonBody(JsBoolean(true)))

      status(result) mustBe NO_CONTENT

      verify(mockFeatureToggleService, times(1))
        .set(toggleName = DummyToggle, enabled = true)
    }

    "not set the feature toggles and return BAD_REQUEST" in {
      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.put(DummyToggle)(fakeRequest.withJsonBody(Json.obj("blah" -> "blah")))

      status(result) mustBe BAD_REQUEST

      verify(mockFeatureToggleService, times(0))
        .set(toggleName = DummyToggle, enabled = true)
    }
  }


  "FeatureToggleController.upsertFeatureToggle" must {
    "set the feature toggles and return NO_CONTENT" in {
      when(mockToggleDataRepository.upsertFeatureToggle(any()))
        .thenReturn(Future.successful(()))

      when(mockFeatureToggleService.upsertFeatureToggle(any()))
        .thenReturn(Future.successful(()))

      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.upsertFeatureToggle(fakeRequest.withJsonBody(Json.obj(
        "toggleName" -> "Test-toggle",
        "toggleDescription" -> "Test description",
        "isEnabled" -> true
      )))

      status(result) mustBe NO_CONTENT

      verify(mockFeatureToggleService, times(1))
        .upsertFeatureToggle(ToggleDetails("Test-toggle", Some("Test description"), isEnabled = true))
    }

    "not set the feature toggles and return BAD_REQUEST" in {
      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.upsertFeatureToggle(fakeRequest)

      status(result) mustBe BAD_REQUEST

      verify(mockFeatureToggleService, times(0))
        .upsertFeatureToggle(ToggleDetails("Test-toggle", Some("Test description"), isEnabled = true))
    }
  }

  "FeatureToggleController.deleteToggle" must {
    "delete the feature toggle and return NO_CONTENT" in {
      when(mockToggleDataRepository.upsertFeatureToggle(any()))
        .thenReturn(Future.successful(()))

      when(mockFeatureToggleService.deleteToggle(any()))
        .thenReturn(Future.successful((): Unit))

      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.deleteToggle("Test toggle")(fakeRequest)

      status(result) mustBe NO_CONTENT

      verify(mockFeatureToggleService, times(1))
        .deleteToggle("Test toggle")
    }
  }

  "FeatureToggleController.getToggle" must {
    "get the feature toggle value and return OK" in {
      when(mockToggleDataRepository.upsertFeatureToggle(any()))
        .thenReturn(Future.successful(()))

      when(mockFeatureToggleService.getToggle(any()))
        .thenReturn(Future.successful(Some(toggleDetails)))

      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.getToggle("Test")(fakeRequest)

      status(result) mustBe OK

      verify(mockFeatureToggleService, times(1))
        .getToggle("Test")
    }
  }

  "FeatureToggleController.getAllFeatureToggles" must {
    "return OK and the feature toggles when they exist" in {

      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.getAllFeatureToggles(fakeRequest)

      status(result) mustBe OK
    }
  }
}
