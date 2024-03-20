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

package services

import org.apache.pekko.Done
import base.SpecBase
import models.FeatureToggle.{Disabled, Enabled}
import models.FeatureToggleName.DummyToggle
import models.{FeatureToggle, FeatureToggleName, ToggleDetails}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import repositories._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class FeatureToggleServiceSpec extends SpecBase with MockitoSugar with ScalaFutures with Matchers {

  implicit private val arbitraryFeatureToggleName: Arbitrary[FeatureToggleName] =
    Arbitrary {
      Gen.oneOf(FeatureToggleName.toggles)
    }

  // A cache that doesn't cache
  class FakeCache extends AsyncCacheApi {
    override def set(key: String, value: Any, expiration: Duration): Future[Done] = ???

    override def remove(key: String): Future[Done] = ???

    override def getOrElseUpdate[A](key: String, expiration: Duration)
                                   (orElse: => Future[A])
                                   (implicit evidence$1: ClassTag[A]): Future[A] = orElse

    override def get[T](key: String)
                       (implicit evidence$2: ClassTag[T]): Future[Option[T]] = ???

    override def removeAll(): Future[Done] = ???
  }

  private val adminDataRepository = mock[AdminDataRepository]
  private val toggleDataRepository: ToggleDataRepository = mock[ToggleDataRepository]

  private val toggleDetails1 = ToggleDetails("Toggle-name1", Some("Toggle description1"), isEnabled = true)
  private val toggleDetails2 = ToggleDetails("Toggle-name2", Some("Toggle description2"), isEnabled = false)
  private val toggleDetails3 = ToggleDetails("Toggle-name3", Some("Toggle description3"), isEnabled = true)
  private val toggleDetails4 = ToggleDetails("Toggle-name4", Some("Toggle description4"), isEnabled = false)

  private val seqToggleDetails = Seq(toggleDetails1, toggleDetails2, toggleDetails3, toggleDetails4)

  private val modules: Seq[GuiceableModule] = Seq(
    bind[LockCacheRepository].toInstance(mock[LockCacheRepository]),
    bind[AdminDataRepository].toInstance(adminDataRepository),
    bind[ToggleDataRepository].toInstance(toggleDataRepository),
    bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
    bind[ListOfLegacySchemesCacheRepository].toInstance(mock[ListOfLegacySchemesCacheRepository]),
    bind[RacDacRequestsQueueRepository].toInstance(mock[RacDacRequestsQueueRepository]),
    bind[SchemeDataCacheRepository].toInstance(mock[SchemeDataCacheRepository]),
    bind[RacDacRequestsQueueEventsLogRepository].toInstance(mock[RacDacRequestsQueueEventsLogRepository]),
    bind[AsyncCacheApi].toInstance(new FakeCache())
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "auditing.enabled" -> false,
      "metrics.enabled" -> false,
      "metrics.jvm" -> false,
      "run.mode" -> "Test"
    ).overrides(modules: _*).build()

  private val OUT: FeatureToggleService = app.injector.instanceOf[FeatureToggleService]

  "When set works in the repo returns a success result" in {
    when(adminDataRepository.getFeatureToggles).thenReturn(Future.successful(Seq.empty))
    when(adminDataRepository.setFeatureToggles(any())).thenReturn(Future.successful((): Unit))

    val toggleName = arbitrary[FeatureToggleName].sample.value

    whenReady(OUT.set(toggleName = toggleName, enabled = true)) {
      _ =>
        val captor = ArgumentCaptor.forClass(classOf[Seq[FeatureToggle]])
        verify(adminDataRepository, times(1)).setFeatureToggles(captor.capture())
        captor.getValue must contain(Enabled(toggleName))
    }
  }

  "When set fails in the repo returns a success result" in {
    val toggleName = arbitrary[FeatureToggleName].sample.value

    when(adminDataRepository.getFeatureToggles).thenReturn(Future.successful(Seq.empty))
    when(adminDataRepository.setFeatureToggles(any())).thenReturn(Future.successful((): Unit))
    whenReady(OUT.set(toggleName = toggleName, enabled = true)) {
      _ mustBe ((): Unit)
    }
  }

  "When getAll is called returns all of the toggles from the repo" in {
    when(adminDataRepository.getFeatureToggles).thenReturn(Future.successful(Seq.empty))

    OUT.getAll.futureValue mustBe Seq(
      Disabled(DummyToggle)
    )
  }

  "When a toggle doesn't exist in the repo, return default" in {
    when(adminDataRepository.getFeatureToggles).thenReturn(Future.successful(Seq.empty))
    OUT.get(DummyToggle).futureValue mustBe Disabled(DummyToggle)
  }

  "When a toggle exists in the repo, override default" in {
    when(adminDataRepository.getFeatureToggles).thenReturn(Future.successful(Seq(Enabled(DummyToggle))))
    OUT.get(DummyToggle).futureValue mustBe Enabled(DummyToggle)
  }

  "When upsertFeatureToggle works in the repo, it returns a success result for the toggle data" in {
    when(toggleDataRepository.getAllFeatureToggles).thenReturn(Future.successful(Seq.empty))
    when(toggleDataRepository.upsertFeatureToggle(any())).thenReturn(Future.successful(()))

    whenReady(OUT.upsertFeatureToggle(toggleDetails1)) {
      result =>
        result mustBe()
        val captor = ArgumentCaptor.forClass(classOf[ToggleDetails])
        verify(toggleDataRepository, times(1)).upsertFeatureToggle(captor.capture())
        captor.getValue mustBe toggleDetails1
    }
  }

  "When deleteToggle works in the repo, it returns a success result for the toggle data" in {
    when(toggleDataRepository.getAllFeatureToggles).thenReturn(Future.successful(Seq.empty))
    when(toggleDataRepository.upsertFeatureToggle(any())).thenReturn(Future.successful(()))
    when(toggleDataRepository.deleteFeatureToggle(any())).thenReturn(Future.successful(()))

    whenReady(OUT.deleteToggle(toggleDetails1.toggleName)) {
      result =>
        result mustBe()
        val captor = ArgumentCaptor.forClass(classOf[String])
        verify(toggleDataRepository, times(1)).deleteFeatureToggle(captor.capture())
        captor.getValue mustBe toggleDetails1.toggleName
    }
  }

  "When getAllFeatureToggles is called returns all of the toggles from the repo" in {
    when(toggleDataRepository.getAllFeatureToggles).thenReturn(Future.successful(seqToggleDetails))
    OUT.getAllFeatureToggles.futureValue mustBe seqToggleDetails
  }

  "When a toggle doesn't exist in the repo, return empty Seq for toggle data repository" in {
    when(toggleDataRepository.getAllFeatureToggles).thenReturn(Future.successful(Seq.empty))
    OUT.getAllFeatureToggles.futureValue mustBe Seq.empty
  }
}
