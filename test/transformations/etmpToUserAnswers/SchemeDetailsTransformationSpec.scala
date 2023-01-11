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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

class SchemeDetailsTransformationSpec extends TransformationSpec {

  val schemeDetailsTransformer: SchemeDetailsTransformer = injector.instanceOf[SchemeDetailsTransformer]

  "An IF payload with Scheme details" must {
    "have the scheme details transformed correctly to valid user answers format" in {

      forAll(schemeDetailsGen) {
        schemeDetails =>
          val (ifSchemeDetails, userAnswersSchemeDetails) = schemeDetails
          val result = ifSchemeDetails.transform(schemeDetailsTransformer.userAnswersSchemeDetailsReads).get
          result mustBe userAnswersSchemeDetails
      }
    }
  }
}
