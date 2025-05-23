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

package base

import config.AppConfig
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.inject.{Injector, bind}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents}
import play.api.test.FakeRequest
import play.api.{Application, Environment}
import repositories._
import uk.gov.hmrc.crypto.ApplicationCrypto

trait SpecBase extends PlaySpec with GuiceOneAppPerSuite with JsonFileReader with MockitoSugar {
  def injector: Injector = app.injector

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      //turn off metrics
      "metrics.jvm" -> false,
      "metrics.enabled" -> false
    )
    .overrides(bindings*)
    .build()

  protected def bindings: Seq[GuiceableModule] = {
    Seq(
      bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
      bind[ListOfLegacySchemesCacheRepository].toInstance(mock[ListOfLegacySchemesCacheRepository]),
      bind[LockCacheRepository].toInstance(mock[LockCacheRepository]),
      bind[RacDacRequestsQueueEventsLogRepository].toInstance(mock[RacDacRequestsQueueEventsLogRepository]),
      bind[RacDacRequestsQueueRepository].toInstance(mock[RacDacRequestsQueueRepository]),
      bind[SchemeDataCacheRepository].toInstance(mock[SchemeDataCacheRepository]),
    )
  }

  def environment: Environment = injector.instanceOf[Environment]

  def appConfig: AppConfig = injector.instanceOf[AppConfig]

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  protected def crypto: ApplicationCrypto = injector.instanceOf[ApplicationCrypto]

  def controllerComponents: ControllerComponents = injector.instanceOf[ControllerComponents]

  protected def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }
}
