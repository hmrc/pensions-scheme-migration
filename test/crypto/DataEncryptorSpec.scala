/*
 * Copyright 2024 HM Revenue & Customs
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

package crypto

import config.AppConfig
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.Json
import repositories._
import uk.gov.hmrc.auth.core.AuthConnector

class DataEncryptorSpec extends AnyFreeSpec with Matchers with BeforeAndAfterAll with MockitoSugar {


  private val modules: Seq[GuiceableModule] = Seq(
    bind[AuthConnector].toInstance(mock[AuthConnector]),
    bind[LockCacheRepository].toInstance(mock[LockCacheRepository]),
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

  private val mockAppConfig = mock[AppConfig]
  private val mockAppConfigNotEncrypted = mock[AppConfig]

  private val encryptor      = new DataEncryptor(
    app.injector.instanceOf[SecureGCMCipher],
    mockAppConfig
  )

  private val encryptorNoKey = new DataEncryptor(
    app.injector.instanceOf[SecureGCMCipher],
    mockAppConfigNotEncrypted
  )

  val secretKey = "QZNWcapID0BmWTneSk4hNl5RqdMlh4RI"

  override def beforeAll(): Unit = {
    super.beforeAll()
    when(mockAppConfig.mongoEncryptionKey).thenReturn(Some(secretKey))
    when(mockAppConfigNotEncrypted.mongoEncryptionKey).thenReturn(None)
  }

  override protected def afterAll(): Unit = {
    app.stop()
    super.afterAll()
  }

  private val id  = "id"
  private val notEncryptedJsValue = Json.parse("""{"value": true}""")
  private val encryptedJsValue  = EncryptedValue("gJBC1pxxAHYb5uUU11m2dzRqWWz50GoBO7FIPwBn",
    "W0cgl1ordmUXazSHdFzlv7McTbGwVa58xxzZYWSuqqjX+tpqm2CDvjZB3E+KhtNQTzFU8HkwcCC5bdgCaPzmG9Qai9AC7lyav9TLd/v9PNNt2f8R3rGsMHf7xxE4dndL")

  "encrypt" - {
    "must encrypt jsValue" in {
      println(encryptor.encrypt(id, notEncryptedJsValue).as[EncryptedValue])
      encryptor.encrypt(id, notEncryptedJsValue).as[EncryptedValue] mustBe an[EncryptedValue]
    }
    "must not encrypt if no key is provided" in {
      encryptorNoKey.encrypt(id, notEncryptedJsValue) mustBe notEncryptedJsValue

    }
  }

  "decrypt" - {
    "must decrypt jsValue" in {
      encryptor.decrypt(id, Json.toJson(encryptedJsValue)) mustBe notEncryptedJsValue
    }

    "must return original value if not encrypted" in {
      encryptorNoKey.decrypt(id, notEncryptedJsValue) mustBe notEncryptedJsValue
    }

    "must throw a RuntimeException if no encryption field is available while decrypting a value" in {
      val exception = intercept[RuntimeException](encryptorNoKey.decrypt(id, Json.toJson(encryptedJsValue)))
      exception mustBe a[RuntimeException]
    }
  }
}
