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

package models.userAnswersToEtmp.reads

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.libs.json.{JsObject, Json}

object CommonGenerator {

  lazy val addressGen: Gen[JsObject] = Gen.oneOf(ukAddressGen, internationalAddressGen)

  def arbitraryString: Gen[String] =  Gen.alphaStr suchThat (_.nonEmpty)

  lazy val ukAddressGen: Gen[JsObject] = for {
    line1 <- arbitraryString
    line2 <- arbitraryString
    line3 <- arbitrary[Option[String]]
    line4 <- arbitrary[Option[String]]
    postalCode <- arbitraryString
  } yield Json.obj("addressLine1" -> line1, "addressLine2" -> line2, "addressLine3" ->line3,
    "addressLine4" -> line4, "country" -> "GB", "postalCode" -> postalCode)

  lazy val internationalAddressGen: Gen[JsObject] = for {
    line1 <- arbitraryString
    line2 <- arbitraryString
    line3 <- arbitrary[Option[String]]
    line4 <- arbitrary[Option[String]]
    countryCode <- arbitraryString
  } yield Json.obj("addressLine1" -> line1, "addressLine2" -> line2, "addressLine3" ->line3,
    "addressLine4" -> line4, "country" -> countryCode)

  def trusteeCompanyGenerator(
                               isDeleted: Boolean = false,
                               email: Option[String] = None
                             ): Gen[JsObject] =
    for {
      companyName <- arbitraryString
      correspondenceAddressDetails <- CommonGenerator.addressGen
      addressYears <- arbitrary[Boolean]
      hasBeenTrading <- arbitrary[Boolean]
      previousAddressDetails <- CommonGenerator.addressGen
      mobileNumber <- arbitraryString
      emailAddress <- email.getOrElse(arbitraryString)
    } yield Json.obj(
      "companyDetails" -> Json.obj(
        "companyName" -> companyName,
        "isDeleted" -> isDeleted
      ),
      "address" -> correspondenceAddressDetails,
      "addressYears" -> addressYears,
      "tradingTime" -> hasBeenTrading,
      "previousAddress" -> previousAddressDetails,
      "email" -> emailAddress.toString,
      "phone" -> mobileNumber,
      "trusteeKind" -> "company"
    )

  def trusteePartnershipGenerator(
                                   isDeleted: Boolean = false,
                                   email: Option[String] = None
                                 ): Gen[JsObject] =
    for {
      emailAddress <- email.getOrElse(arbitraryString)
      phoneNumber <- arbitraryString
      addressDetails <- CommonGenerator.addressGen
      previousAddressDetails <- CommonGenerator.addressGen
      name <- arbitraryString
      addressYears <- arbitrary[Boolean]
    } yield {
      Json.obj(
        "isTrusteeNew" -> true,
          "partnershipEmail" -> emailAddress.toString,
          "partnershipPhone" -> phoneNumber,
        "previousAddress" -> previousAddressDetails,
        "address" -> addressDetails,
        "trusteeKind" -> "partnership",
        "partnershipDetails" -> Json.obj(
          "partnershipName" -> name,
          "isDeleted" -> isDeleted
        ),
        "addressYears" -> addressYears,
        "trusteeKind" -> "partnership"
      )
    }

  def trusteeIndividualGenerator(
                                  isDeleted: Boolean = false,
                                  email: Option[String] = None
                                ): Gen[JsObject] =
    for {
      firstName <- arbitraryString
      lastName <- arbitraryString
      dateOfBirth <- arbitraryString
      correspondenceAddressDetails <- CommonGenerator.addressGen
      addressYears <- arbitrary[Boolean]
      previousAddressDetails <- CommonGenerator.addressGen
      mobileNumber <- arbitraryString
      emailAddress <- email.getOrElse(arbitraryString)
    } yield Json.obj(
      "trusteeDetails" -> Json.obj(
        "firstName" -> firstName,
        "lastName" -> lastName,
        "isDeleted" -> isDeleted
      ),
      "dateOfBirth" -> dateOfBirth,
      "address" -> correspondenceAddressDetails,
      "addressYears" -> addressYears,
      "previousAddress" -> previousAddressDetails,
        "emailAddress" -> emailAddress.toString,
        "phoneNumber" -> mobileNumber,
      "trusteeKind" -> "individual"
    )

  val trusteesGen: Gen[JsObject] =
    for {
      company1 <- trusteeCompanyGenerator()
      company2 <- trusteeCompanyGenerator(true)
      individual3 <- trusteeIndividualGenerator(true)
      individual4 <- trusteeIndividualGenerator()
      partnership5 <- trusteePartnershipGenerator()
      partnership6 <- trusteePartnershipGenerator(true)
    } yield {
      Json.obj(
        "trustees" -> Seq(
          company1, company2, individual3, individual4, partnership5, partnership6
        )
      )
    }

  def directorGenerator(
                         isDeleted: Boolean = false,
                         email: Option[String] = None
                       ): Gen[JsObject] =
    for {
      firstName <- arbitraryString
      lastName <- arbitraryString
      dateOfBirth <- arbitraryString
      correspondenceAddressDetails <- CommonGenerator.addressGen
      addressYears <- arbitrary[Boolean]
      previousAddressDetails <- CommonGenerator.addressGen
      mobileNumber <- arbitraryString
      emailAddress <- email.getOrElse(arbitraryString)
    } yield Json.obj(
      "directorDetails" -> Json.obj(
        "firstName" -> firstName,
        "lastName" -> lastName,
        "isDeleted" -> isDeleted
      ),
      "dateOfBirth" -> dateOfBirth,
      "address" -> correspondenceAddressDetails,
      "addressYears" -> addressYears,
      "previousAddress" -> previousAddressDetails,
      "directorContactDetails" -> Json.obj(
        "emailAddress" -> emailAddress.toString,
        "phoneNumber" -> mobileNumber
      )
    )

  def partnerGenerator(
                        isDeleted: Boolean = false,
                        email: Option[String] = None
                      ): Gen[JsObject] =
    for {
      firstName <- arbitraryString
      lastName <- arbitraryString
      dateOfBirth <- arbitraryString
      correspondenceAddressDetails <- CommonGenerator.addressGen
      addressYears <- arbitrary[Boolean]
      previousAddressDetails <- CommonGenerator.addressGen
      mobileNumber <- arbitraryString
      emailAddress <- email.getOrElse(arbitraryString)
    } yield Json.obj(
      "partnerDetails" -> Json.obj(
        "firstName" -> firstName,
        "lastName" -> lastName,
        "isDeleted" -> isDeleted
      ),
      "dateOfBirth" -> dateOfBirth,
      "address" -> correspondenceAddressDetails,
      "addressYears" -> addressYears,
      "previousAddress" -> previousAddressDetails,
      "partnerContactDetails" -> Json.obj(
        "emailAddress" -> emailAddress.toString,
        "phoneNumber" -> mobileNumber
      )
    )

  def establisherIndividualGenerator(
                                      isDeleted: Boolean = false,
                                      email: Option[String] = None
                                    ): Gen[JsObject] =
    for {
      firstName <- arbitraryString
      lastName <- arbitraryString
      dateOfBirth <- arbitraryString
      correspondenceAddressDetails <- CommonGenerator.addressGen
      addressYears <- arbitrary[Boolean]
      previousAddressDetails <- CommonGenerator.addressGen
      mobileNumber <- arbitraryString
      emailAddress <- email.getOrElse(arbitraryString)
    } yield Json.obj(
      "establisherDetails" -> Json.obj(
        "firstName" -> firstName,
        "lastName" -> lastName,
        "isDeleted" -> isDeleted
      ),
      "dateOfBirth" -> dateOfBirth,
      "address" -> correspondenceAddressDetails,
      "addressYears" -> addressYears,
      "previousAddress" -> previousAddressDetails,
        "emailAddress" -> emailAddress.toString,
        "phoneNumber" -> mobileNumber,
      "establisherKind" -> "individual"
    )

  def establisherCompanyGenerator(
                                   isDeleted: Boolean = false,
                                   email: Option[String] = None
                                 ): Gen[JsObject] =
    for {
      companyName <- arbitraryString
      correspondenceAddressDetails <- CommonGenerator.addressGen
      addressYears <- arbitrary[Boolean]
      previousAddressDetails <- CommonGenerator.addressGen
      mobileNumber <- arbitraryString
      emailAddress <- email.getOrElse(arbitraryString)
      otherDirectors <- arbitrary[Boolean]
      director1 <- directorGenerator()
      director2 <- directorGenerator(true)
      director3 <- directorGenerator()
    } yield Json.obj(
      "companyDetails" -> Json.obj(
        "companyName" -> companyName,
        "isDeleted" -> isDeleted
      ),
      "address" -> correspondenceAddressDetails,
      "addressYears" -> addressYears,
      "previousAddress" -> previousAddressDetails,
        "email" -> emailAddress.toString,
        "phone" -> mobileNumber,
      "otherDirectors" -> otherDirectors,
      "director" -> Seq(
        director1, director2, director3
      ),
      "establisherKind" -> "company"
    )

  def establisherPartnershipGenerator(
                                       isDeleted: Boolean = false,
                                       email: Option[String] = None
                                     ): Gen[JsObject] =
    for {
      emailAddress <- email.getOrElse(arbitraryString)
      phoneNumber <- arbitraryString
      addressDetails <- CommonGenerator.addressGen
      previousAddressDetails <- CommonGenerator.addressGen
      name <- arbitraryString
      addressYears <- arbitrary[Boolean]
      otherPartners <- arbitrary[Boolean]
      partner1 <- partnerGenerator()
      partner2 <- partnerGenerator()
      partner3 <- partnerGenerator(true)
    } yield {
      Json.obj(
        "isEstablisherNew" -> true,
          "partnershipEmail" -> emailAddress.toString,
          "partnershipPhone" -> phoneNumber,
        "previousAddress" -> previousAddressDetails,
        "address" -> addressDetails,
        "establisherKind" -> "partnership",
        "partnershipDetails" -> Json.obj(
          "partnershipName" -> name,
          "isDeleted" -> isDeleted
        ),
        "addressYears" -> addressYears,
        "otherPartners" -> otherPartners,
        "partner" -> Seq(
          partner1, partner2, partner3
        ),
        "establisherKind" -> "partnership"
      )
    }

  val establishersGen: Gen[JsObject] =
    for {
      company1 <- establisherCompanyGenerator()
      company2 <- establisherCompanyGenerator(true)
      individual3 <- establisherIndividualGenerator(true)
      individual4 <- establisherIndividualGenerator()
      partnership5 <- establisherPartnershipGenerator()
      partnership6 <- establisherPartnershipGenerator(true)
    } yield {
      Json.obj(
        "establishers" -> Seq(
          company1, company2, individual3, individual4, partnership5, partnership6
        )
      )
    }
}
