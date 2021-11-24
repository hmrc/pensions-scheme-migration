/*
 * Copyright 2021 HM Revenue & Customs
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

package models.userAnswersToEtmp.reads.trustees

import models.userAnswersToEtmp.reads.CommonGenerator
import models.userAnswersToEtmp.reads.CommonGenerator.trusteesGen
import models.userAnswersToEtmp.trustee.TrusteeDetails
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class ReadsTrusteeDetailsSpec extends AnyWordSpec with Matchers with OptionValues {

  "ReadsTrusteeDetails" must {

    "read multiple trustees with filtering out the deleted ones" in {
      forAll(trusteesGen) { json =>
        val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

        trusteeDetails.companyTrusteeDetail.head.organizationName mustBe
          (json \ "trustees" \ 0 \ "companyDetails" \ "companyName").as[String]

        trusteeDetails.individualTrusteeDetail.head.personalDetails.firstName mustBe
          (json \ "trustees" \ 3 \ "trusteeDetails" \ "firstName").as[String]

        trusteeDetails.partnershipTrusteeDetail.head.organizationName mustBe
          (json \ "trustees" \ 4 \ "partnershipDetails" \ "partnershipName").as[String]
      }
    }



    "read individual trustee which includes a companyDetails node (due to url manipulation - fix for production issue)" in {
      forAll(CommonGenerator.trusteeIndividualGenerator()) { individualTrustee =>
        val json = Json.obj(
          "trustees" -> Json.arr(
            individualTrustee ++ Json.obj(
              "companyDetails" -> Json.obj()
            )
          )
        )

        val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

        trusteeDetails.individualTrusteeDetail.head.personalDetails.firstName mustBe
          (json \ "trustees" \ 0 \ "trusteeDetails" \ "firstName").as[String]
      }
    }

    "read individual trustee which includes a partnershipDetails node (due to url manipulation - fix for production issue)" in {
      forAll(CommonGenerator.trusteeIndividualGenerator()) { individualTrustee =>
        val json = Json.obj(
          "trustees" -> Json.arr(
            individualTrustee ++ Json.obj(
              "partnershipDetails" -> Json.obj()
            )
          )
        )

        val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

        trusteeDetails.individualTrusteeDetail.head.personalDetails.firstName mustBe
          (json \ "trustees" \ 0 \ "trusteeDetails" \ "firstName").as[String]
      }
    }

    "read company trustee which includes an trusteeDetails node (due to url manipulation - fix for production issue)" in {
      forAll(CommonGenerator.trusteeCompanyGenerator()) { companyTrustee =>
        val json = Json.obj(
          "trustees" -> Json.arr(
            companyTrustee ++ Json.obj(
              "trusteeDetails" -> Json.obj()
            )
          )
        )

        val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

        trusteeDetails.companyTrusteeDetail.head.organizationName mustBe
          (json \ "trustees" \ 0 \ "companyDetails" \ "companyName").as[String]
      }
    }

    "read partnership trustee which includes an trusteeDetails node (due to url manipulation - fix for production issue)" in {
      forAll(CommonGenerator.trusteePartnershipGenerator()) { partnershipTrustee =>
        val json = Json.obj(
          "trustees" -> Json.arr(
            partnershipTrustee ++ Json.obj(
              "trusteeDetails" -> Json.obj()
            )
          )
        )

        val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

        trusteeDetails.partnershipTrusteeDetail.head.organizationName mustBe
          (json \ "trustees" \ 0 \ "partnershipDetails" \ "partnershipName").as[String]
      }
    }
  }

  "read only 1 of 2 company trustees of which one includes only an trusteeKind node and " +
    "an isTrusteeNew node (due to not entering a name - fix for production issue)" in {
    forAll(CommonGenerator.trusteeCompanyGenerator()) { companyTrustee =>
      val json = Json.obj(
        "trustees" -> Json.arr(
          companyTrustee,
          Json.obj(
            "trusteeKind" -> "company",
            "isTrusteeNew" -> true
          )
        )
      )

      val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

      trusteeDetails.companyTrusteeDetail.head.organizationName mustBe
        (json \ "trustees" \ 0 \ "companyDetails" \ "companyName").as[String]
      trusteeDetails.companyTrusteeDetail.size mustBe 1

    }
  }

  "read only 1 of 2 individual trustees of which one includes only an trusteeKind node and " +
    "an isTrusteeNew node (due to not entering a name - fix for production issue)" in {
    forAll(CommonGenerator.trusteeIndividualGenerator()) { individualTrustee =>
      val json = Json.obj(
        "trustees" -> Json.arr(
          individualTrustee,
          Json.obj(
            "trusteeKind" -> "individual",
            "isTrusteeNew" -> true
          )
        )
      )

      val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

      trusteeDetails.individualTrusteeDetail.head.personalDetails.firstName mustBe
        (json \ "trustees" \ 0 \ "trusteeDetails" \ "firstName").as[String]
      trusteeDetails.individualTrusteeDetail.size mustBe 1

    }
  }

  "read only 1 of 2 partnership trustees of which one includes only an trusteeKind node and " +
    "an isTrusteeNew node (due to not entering a name - fix for production issue)" in {
    forAll(CommonGenerator.trusteePartnershipGenerator()) { partnershipTrustee =>
      val json = Json.obj(
        "trustees" -> Json.arr(
          partnershipTrustee,
          Json.obj(
            "trusteeKind" -> "partnership",
            "isTrusteeNew" -> true
          )
        )
      )

      val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

      trusteeDetails.partnershipTrusteeDetail.head.organizationName mustBe
        (json \ "trustees" \ 0 \ "partnershipDetails" \ "partnershipName").as[String]
      trusteeDetails.partnershipTrusteeDetail.size mustBe 1

    }
  }
}
