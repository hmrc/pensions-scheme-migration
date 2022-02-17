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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.Json

class EstablisherDetailsTransformerSpec extends TransformationSpec {

  private val addressTransformer = new AddressTransformer
  private val transformer = new EstablisherDetailsTransformer(addressTransformer, countryOptions)

  "An if payload containing establisher details" must {
//    "have the individual details transformed correctly to valid user answers format" that {
//
//      s"has person details in establishers array" in {
//        forAll(individualJsValueGen(isEstablisher = true)) {
//          individualDetails => {
//            val details = individualDetails._1
//            val result = details.transform(transformer.userAnswersIndividualDetailsReads("establisherDetails")).get
//
//            (result \ "establisherDetails" \ "firstName").as[String] mustBe (details \ "personDetails" \ "firstName").as[String]
//            (result \ "establisherDetails" \ "lastName").as[String] mustBe (details \ "personDetails" \ "lastName").as[String]
//            (result \ "dateOfBirth").as[String] mustBe (details \ "personDetails" \ "dateOfBirth").as[String]
//          }
//        }
//      }
//
//      s"has nino details in establishers array" in {
//        forAll(individualJsValueGen(isEstablisher = true)) {
//          individualDetails => {
//            val details = individualDetails._1
//            val result = details.transform(transformer.userAnswersNinoReads).get
//
//            (result \ "nino" \ "value").asOpt[String] mustBe (details \ "nino").asOpt[String]
//            (result \ "noNinoReason").asOpt[String] mustBe (details \ "noNinoReason").asOpt[String]
//          }
//        }
//      }
//
//      s"has contact details in establishers array" in {
//        forAll(individualJsValueGen(isEstablisher = true)) {
//          individualDetails => {
//            val details = individualDetails._1
//            val result = details.transform(transformer.userAnswersContactDetailsReads).get
//
//            (result \ "email").as[String] mustBe (details \ "correspContDetails" \ "email").as[String]
//            (result  \ "phone").as[String] mustBe
//              (details \ "correspContDetails" \ "telephone").as[String]
//          }
//        }
//      }
//
//      "has complete individual details" in {
//        forAll(individualJsValueGen(isEstablisher = true)) {
//          individualDetails => {
//            val (ifIndividualDetails, userAnswersIndividualDetails) = individualDetails
//            val details = ifIndividualDetails
//
//            val result = details.transform(transformer.userAnswersEstablisherIndividualReads).get
//            result mustBe userAnswersIndividualDetails
//          }
//        }
//      }
//    }
//
//    "have the companyOrOrganisationDetails details for company transformed correctly to valid user answers format for first json file" that {
//
//      s"has establisher details in establishers array" in {
//        forAll(companyJsValueGen(isEstablisher = true)) {
//          companyDetails => {
//            val details = companyDetails._1
//            val result = details.transform(transformer.userAnswersCompanyDetailsReads).get
//
//            (result \ "companyDetails" \ "companyName").as[String] mustBe (details \ "comOrOrganisationName").as[String]
//          }
//        }
//      }
//
//      s"has vat details for company in establishers array" in {
//        forAll(companyJsValueGen(isEstablisher = true)) {
//          companyDetails => {
//            val details = companyDetails._1
//            val result = details.transform(transformer.userAnswersVatReads).get
//
//            (result \ "vat" \ "value").asOpt[String] mustBe (details \ "vatRegistrationNumber").asOpt[String]
//          }
//        }
//      }
//
//      s"has paye details for company in establishers array" in {
//        forAll(companyJsValueGen(isEstablisher = true)) {
//          companyDetails => {
//            val details = companyDetails._1
//            val result = details.transform(transformer.userAnswersPayeReads).get
//
//            (result \ "paye" \ "value").asOpt[String] mustBe (details \ "payeReference").asOpt[String]
//          }
//        }
//      }
//
//      s"has crn details in establishers array" in {
//        forAll(companyJsValueGen(isEstablisher = true)) {
//          companyDetails => {
//            val details = companyDetails._1
//            val result = details.transform(transformer.userAnswersCrnReads).get
//
//            (result \ "noCompanyNumberReason").asOpt[String] mustBe (details \ "noCrnReason").asOpt[String]
//            (result \ "companyNumber" \ "value").asOpt[String] mustBe (details \ "crnNumber").asOpt[String]
//          }
//        }
//      }
//
//      s"has contact details in establishers array" in {
//        forAll(companyJsValueGen(isEstablisher = true)) {
//          companyDetails => {
//            val details = companyDetails._1
//            val result = details.transform(transformer.userAnswersContactDetailsReads).get
//
//            (result \ "email").as[String] mustBe
//              (details \ "correspContDetails" \ "email").as[String]
//            (result \ "phone").as[String] mustBe
//              (details \ "correspContDetails" \ "telephone").as[String]
//          }
//        }
//      }
//
//      s"has complete company details in establishers array" in {
//        forAll(companyJsValueGen(isEstablisher = true)) {
//          companyDetails => {
//            val (ifCompanyDetails, userAnswersCompanyDetails) = companyDetails
//            val details = ifCompanyDetails
//            val result = details.transform(transformer.userAnswersEstablisherCompanyReads).get
//
//            result mustBe userAnswersCompanyDetails
//          }
//        }
//      }
//    }

    "have all establishers transformed" in {
      forAll(establisherOrTrusteeJsValueGen(isEstablisher = true)) {
        establishers =>
          val (ifEstablishers, uaEstablishers) = establishers
          val result = ifEstablishers.transform(transformer.userAnswersEstablishersReads).get
          result mustBe uaEstablishers
      }
    }

//    "if no establishers are present" in {
//      val result = Json.obj().transform(transformer.userAnswersEstablishersReads).get
//
//      result mustBe Json.obj()
//   }
  }
}

