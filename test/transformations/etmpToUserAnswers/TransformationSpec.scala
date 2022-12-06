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

package transformations.etmpToUserAnswers

import base.JsonFileReader
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.{Configuration, Environment}
import repositories._
import utils.{CountryOptions, PensionSchemeJsValueGenerators}

trait TransformationSpec extends AnyWordSpec with Matchers with OptionValues with JsonFileReader with PensionSchemeJsValueGenerators with MockitoSugar {

  val injector: Injector = new GuiceApplicationBuilder().overrides(
    bind[AdminDataRepository].toInstance(mock[AdminDataRepository]),
    bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
    bind[ListOfLegacySchemesCacheRepository].toInstance(mock[ListOfLegacySchemesCacheRepository]),
    bind[LockCacheRepository].toInstance(mock[LockCacheRepository]),
    bind[RacDacRequestsQueueEventsLogRepository].toInstance(mock[RacDacRequestsQueueEventsLogRepository]),
    bind[RacDacRequestsQueueRepository].toInstance(mock[RacDacRequestsQueueRepository]),
    bind[SchemeDataCacheRepository].toInstance(mock[SchemeDataCacheRepository])
  ).build().injector

  val config: Configuration = injector.instanceOf[Configuration]
  val environment: Environment = injector.instanceOf[Environment]
  val countryOptions: CountryOptions = injector.instanceOf[CountryOptions]
}
