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

package models

import config.AppConfig
import crypto.{DataEncryptor, SecureGCMCipher}
import models.racDac.{EncryptedWorkItemRequest, RacDacHeaders, RacDacRequest, WorkItemRequest}
import org.mockito.Mockito
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json

class WorkItemRequestSpec extends AnyWordSpec
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MockitoSugar {

  private val mockAppConfig = mock[AppConfig]
  private def dataEncryptor = new DataEncryptor(new SecureGCMCipher(), mockAppConfig)
  private val racDacRequest = WorkItemRequest("test psa",
    RacDacRequest("test scheme 1", "001","00615269RH","2012-02-20","2020-01-01"), RacDacHeaders(None, None))
  override def beforeEach(): Unit = {
    Mockito.when(mockAppConfig.mongoEncryptionKey).thenReturn(Some("gvBoGdgzqG1AarzF1LY0zQ=="))
    super.beforeEach()
  }

  "WorkItemRequestSpec" when {
    "EncryptedWorkItemRequest" must {
      "be able to json read encrypted values" in {
        val result = Json.toJson(racDacRequest.encrypt(dataEncryptor)).as[EncryptedWorkItemRequest].decrypt(dataEncryptor)
        result mustBe racDacRequest
      }

      "be able to json read not-encrypted values" in {
        val result = Json.toJson(racDacRequest).as[EncryptedWorkItemRequest].decrypt(dataEncryptor)
        result mustBe racDacRequest
      }
    }
  }
}
