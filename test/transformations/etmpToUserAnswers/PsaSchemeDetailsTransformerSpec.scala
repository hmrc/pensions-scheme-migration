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

package transformations.etmpToUserAnswers

import play.api.libs.json.{JsSuccess, JsValue}

class PsaSchemeDetailsTransformerSpec extends TransformationSpec {

  val transformer: PsaSchemeDetailsTransformer = injector.instanceOf[PsaSchemeDetailsTransformer]

  private val ifResponse: JsValue =
    readJsonFromFile("/data/validGetSchemeDetailsResponse.json")
  private val userAnswersResponse: JsValue =
    readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswers.json")

  "A payload with full scheme subscription details " must {
    "have the details transformed correctly to valid user answers format" which {
      s"uses request/response json" in {
        val result = ifResponse.transform(transformer.transformToUserAnswers)
        result mustBe JsSuccess(userAnswersResponse)
      }
    }
  }
}
