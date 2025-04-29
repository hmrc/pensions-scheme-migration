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

package controllers.actions

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{FORBIDDEN, OK}
import play.api.mvc.AnyContent
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import service.PensionSchemeService
import uk.gov.hmrc.domain.PsaId
import utils.AuthUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeActionSpec extends PlaySpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach {

  private val mockPensionSchemeService = mock[PensionSchemeService]
  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPensionSchemeService)
  }

  private val authRequest = AuthRequest[AnyContent](FakeRequest("", ""), AuthUtils.psaId,  AuthUtils.externalId)

  private val pstr = "pstr"
  private def getResult = {
    new SchemeAuthAction(mockPensionSchemeService)
      .apply(pstr)
      .invokeBlock(authRequest, { (_: AuthRequest[AnyContent]) => Future.successful(Ok("success")) })
  }


  private def mockCheckForAssociation = {
    when(mockPensionSchemeService.isAssociated(ArgumentMatchers.eq(PsaId(AuthUtils.psaId)), ArgumentMatchers.eq(pstr))(ArgumentMatchers.any()))
  }

  "PsaSchemeActionSpec" must {
    "return success response if pension scheme is associated with srn" in {
      mockCheckForAssociation.thenReturn(Future.successful(true))
      val result = getResult
      status(result) mustBe OK
      contentAsString(result) mustBe "success"
    }

    "return Forbidden if pension scheme is not associated with srn" in {
      mockCheckForAssociation.thenReturn(Future.successful(false))
      status(getResult) mustBe FORBIDDEN
    }
  }
}
