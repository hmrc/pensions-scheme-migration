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

package utils

import models.enumeration.{SchemeMembers, SchemeType}
import org.joda.time.LocalDate
import org.scalacheck.Gen
import play.api.libs.json.Reads._
import play.api.libs.json.{JsObject, Json}

trait PensionSchemeJsValueGenerators extends PensionSchemeGenerators {

  private val contactDetailsJsValueGen = for {
    email <- Gen.const("aaa@gmail.com")
    phone <- Gen.listOfN[Char](randomNumberFromRange(1, 24), Gen.numChar).map(_.mkString)
  } yield {
    (
      Json.obj(
        "email" -> email,
        "telephone" -> phone
      ),
      Json.obj(
        "email" -> email,
        "phone" -> phone
      )
    )
  }

  def addressJsValueGen (ifKey: String = "ifAddress", uaKey: String = "userAnswersAddress"): Gen[(JsObject, JsObject)] = for {
    line1 <- addressLineGen
    line2 <- addressLineGen
    line3 <- addressLineOptional
    line4 <- addressLineOptional
    postalCode <- optionalPostalCodeGen
    country <- countryName
    countryCode <- countryCode
  } yield {
    (
      Json.obj(
        ifKey -> (Json.obj("addressLine1" -> line1) ++
          Json.obj("addressLine2" -> line2) ++
            optional("addressLine3", line3) ++
              optional("addressLine4", line4) ++
                optional( "postalCode", postalCode) ++
                  Json.obj("country" -> country))
      ),
      Json.obj(
        uaKey -> (Json.obj("addressLine1" -> line1) ++
          Json.obj("addressLine2" -> line2) ++
            optional("addressLine3", line3) ++
              optional("addressLine4", line4) ++
                optional( "postcode", postalCode) ++
                  Json.obj("country" -> countryCode)))
    )

  }

  def individualJsValueGen(isEstablisher: Boolean): Gen[(JsObject, JsObject)] = for {
    firstName <- nameGenerator
    lastName <- nameGenerator
    referenceOrNino <- Gen.option(ninoGenerator)
    contactDetails <- contactDetailsJsValueGen
    address <- addressJsValueGen("correspAddrDetails", "address")
    date <- dateGenerator
  } yield {
    val (ifAddress, userAnswersAddress) = address
    val (ifContactDetails, userAnswersContactDetails) = contactDetails
    (
      Json.obj(
        "personDetails" -> Json.obj(
          "firstName" -> firstName,
          "lastName" -> lastName,
          "dateOfBirth" -> date.toString
        ),
        "correspContDetails" -> ifContactDetails
      ) ++ ifAddress.as[JsObject] ++ optionalWithReason("nino", referenceOrNino, "noNinoReason"),
      Json.obj(
        (if (isEstablisher) "establisherKind" else "trusteeKind") -> "individual")++
        userAnswersContactDetails ++ userAnswersAddress.as[JsObject] ++
        (if (!isEstablisher) getPersonName(firstName, lastName, date, "trusteeDetails")
        else
          getPersonName(firstName, lastName, date, "establisherDetails")
        ) ++ ninoJsValue(referenceOrNino, "nino"))
  }

  private def getPersonName(firstName: String, lastName: String, date: LocalDate, element: String) = {
    Json.obj(
      element -> Json.obj(
        "firstName" -> firstName,
        "lastName" -> lastName
      ),
      "dateOfBirth" -> date.toString)
  }

  // scalastyle:off method.length
  //scalastyle:off cyclomatic.complexity
  def companyJsValueGen(isEstablisher: Boolean): Gen[(JsObject, JsObject)] = for {
    orgName <- nameGenerator
    crn <- Gen.option(crnGenerator)
    vat <- Gen.option(vatGenerator)
    paye <- Gen.option(payeGenerator)
    address <- addressJsValueGen("correspAddrDetails", "address")
    contactDetails <- contactDetailsJsValueGen
  } yield {
   (
      Json.obj(
        "comOrOrganisationName" -> orgName,
        "correspContDetails" -> contactDetails._1,
        )
        ++ optionalWithReason("crnNumber", crn, "noCrnReason")
        ++ optional("vatRegistrationNumber", vat)
        ++ optional("payeReference", paye)
        ++ address._1.as[JsObject],
      Json.obj(
        (if (isEstablisher) "establisherKind" else "trusteeKind") -> "company",
        "companyDetails" -> Json.obj(
          "companyName" -> orgName
        ))
        ++ contactDetails._2
        ++ vatJsValue(vat, "vat")
        ++ payeJsValue(paye, "paye")
        ++ crnJsValue(crn, "companyNumber")
        ++ address._2.as[JsObject]
    )
  }

  def establisherOrTrusteeJsValueGen(isEstablisher: Boolean): Gen[(JsObject, JsObject)] = for {
    individual <- Gen.option(Gen.listOfN(randomNumberFromRange(1, 1), individualJsValueGen(isEstablisher)))
    company <- Gen.option(Gen.listOfN(randomNumberFromRange(1, 1), companyJsValueGen(isEstablisher)))
  } yield {
    val uaIndividualDetails = individual.map { indv => indv.map(_._2) }.getOrElse(Nil)
    val uaCompanyDetails = company.map { comp => comp.map(_._2) }.getOrElse(Nil)

    val ifEstablishers = individual.map { indv => Json.obj("individualDetails" -> indv.map(_._1)) }.getOrElse(Json.obj()) ++
      company.map { comp => Json.obj("companyOrOrgDetails" -> comp.map(_._1)) }.getOrElse(Json.obj())

    val ifTrustees = individual.map { indv => Json.obj("individualDetails" -> indv.map(_._1)) }.getOrElse(Json.obj()) ++
      company.map { comp => Json.obj("companyOrOrgDetails" -> comp.map(_._1)) }.getOrElse(Json.obj())

    val ifEstablishersJson = Json.obj(
        "schemeEstablishers" -> ifEstablishers
      )

    val ifTrusteesJson = Json.obj(
        "schemeTrustees" -> ifTrustees
    )
    val lisOfAllUserAnswersEstablishers = uaIndividualDetails ++ uaCompanyDetails
    (
      Json.obj("items" -> { if (isEstablisher) ifEstablishersJson else ifTrusteesJson}),
      Json.obj(
        (if (isEstablisher) "establishers" else "trustees") -> lisOfAllUserAnswersEstablishers
      )
    )
  }
  def establisherOrTrusteeJsValueGenForScheme(isEstablisher: Boolean): Gen[(JsObject, JsObject)] = for {
    individual <- Gen.option(Gen.listOfN(randomNumberFromRange(1, 1), individualJsValueGen(isEstablisher)))
    company <- Gen.option(Gen.listOfN(randomNumberFromRange(1, 1), companyJsValueGen(isEstablisher)))
  } yield {
    val uaIndividualDetails = individual.map { indv => indv.map(_._2) }.getOrElse(Nil)
    val uaCompanyDetails = company.map { comp => comp.map(_._2) }.getOrElse(Nil)

    val ifEstablishers = individual.map { indv => Json.obj("individualDetails" -> indv.map(_._1)) }.getOrElse(Json.obj()) ++
      company.map { comp => Json.obj("companyOrOrgDetails" -> comp.map(_._1)) }.getOrElse(Json.obj())

    val ifTrustees = individual.map { indv => Json.obj("individualDetails" -> indv.map(_._1)) }.getOrElse(Json.obj()) ++
      company.map { comp => Json.obj("companyOrOrgDetails" -> comp.map(_._1)) }.getOrElse(Json.obj())

    val ifEstablishersJson = Json.obj(
      "schemeEstablishers" -> ifEstablishers
    )

    val ifTrusteesJson = Json.obj(
      "schemeTrustees" -> ifTrustees
    )
    val lisOfAllUserAnswersEstablishers = uaIndividualDetails ++ uaCompanyDetails
    (
      if (isEstablisher) ifEstablishersJson else ifTrusteesJson,
      Json.obj(
        (if (isEstablisher) "establishers" else "trustees") -> lisOfAllUserAnswersEstablishers
      )
    )
  }

  val schemeDetailsGenForScheme: Gen[(JsObject, JsObject)] = for {
    schemeName <- specialCharStringGen
    pensionSchemeStructure <- schemeTypeGen
    schemeEstablishedCountry <- countryName
    schemeEstablishedCode <- countryCode
    currentSchemeMembers <- memberGen
    isRegulatedSchemeInvestment <- boolenGen
    isOccupationalPensionScheme <- boolenGen
    isSchemeBenefitsInsuranceCompany <- boolenGen
    racDac <- boolenGen
    date <- dateGenerator
  } yield {
    (Json.obj(
      "schemeName" -> schemeName,
      "pensionSchemeStructure" -> pensionSchemeStructure,
      "schemeEstablishedCountry" -> schemeEstablishedCountry,
      "currentSchemeMembers" -> currentSchemeMembers,
      "isRegulatedSchemeInvestment" -> isRegulatedSchemeInvestment,
      "isOccupationalPensionScheme" -> isOccupationalPensionScheme,
      "isSchemeBenefitsInsuranceCompany" -> isSchemeBenefitsInsuranceCompany,
      "racDac" -> racDac,
      "relationshipStartDate" -> date.toString,
      "schemeOpenDate" -> date.toString
    ),
      Json.obj(
        "schemeName" -> schemeName,
        "schemeType" -> Json.obj("name" -> SchemeType.nameWithValue(pensionSchemeStructure.getOrElse("other"))),
        "schemeEstablishedCountry" -> schemeEstablishedCode,
        "currentMembers" -> SchemeMembers.nameWithValue(currentSchemeMembers),
        "investmentRegulated" -> isRegulatedSchemeInvestment,
        "occupationalPensionScheme" -> isOccupationalPensionScheme,
        "securedBenefits" -> isSchemeBenefitsInsuranceCompany,
        "racDac" -> racDac,
        "relationshipStartDate" -> date.toString,
        "schemeOpenDate" -> date.toString
      ))

  }


  val schemeDetailsGen: Gen[(JsObject, JsObject)] = for {
    schemeName <- specialCharStringGen
    pensionSchemeStructure <- schemeTypeGen
    schemeEstablishedCountry <- countryName
    schemeEstablishedCode <- countryCode
    currentSchemeMembers <- memberGen
    isRegulatedSchemeInvestment <- boolenGen
    isOccupationalPensionScheme <- boolenGen
    isSchemeBenefitsInsuranceCompany <- boolenGen
    racDac <- boolenGen
    date <- dateGenerator
  } yield {
    (Json.obj("items" ->Json.obj(
      "schemeName" -> schemeName,
      "pensionSchemeStructure" -> pensionSchemeStructure,
      "schemeEstablishedCountry" -> schemeEstablishedCountry,
      "currentSchemeMembers" -> currentSchemeMembers,
      "isRegulatedSchemeInvestment" -> isRegulatedSchemeInvestment,
      "isOccupationalPensionScheme" -> isOccupationalPensionScheme,
      "isSchemeBenefitsInsuranceCompany" -> isSchemeBenefitsInsuranceCompany,
      "racDac" -> racDac,
      "relationshipStartDate" -> date.toString,
      "schemeOpenDate" -> date.toString
    )),
      Json.obj(
        "schemeName" -> schemeName,
        "schemeType" -> Json.obj("name" -> SchemeType.nameWithValue(pensionSchemeStructure.getOrElse("other"))),
        "schemeEstablishedCountry" -> schemeEstablishedCode,
        "currentMembers" -> SchemeMembers.nameWithValue(currentSchemeMembers),
        "investmentRegulated" -> isRegulatedSchemeInvestment,
        "occupationalPensionScheme" -> isOccupationalPensionScheme,
        "securedBenefits" -> isSchemeBenefitsInsuranceCompany,
        "racDac" -> racDac,
        "relationshipStartDate" -> date.toString,
        "schemeOpenDate" -> date.toString
      ))

  }


  def getSchemeDetailsGen: Gen[(JsObject, JsObject)] = for {
    schemeDetails <- schemeDetailsGenForScheme
    establishers <- Gen.option(establisherOrTrusteeJsValueGenForScheme(isEstablisher = true))
    trustees <- Gen.option(establisherOrTrusteeJsValueGenForScheme(isEstablisher = false))
  } yield {
    val (ifSchemeDetails, uaSchemeDetails) = schemeDetails
    val ifEstablishers = establishers.map(_._1).getOrElse(Json.obj())
    val ifTrustee = trustees.map(_._1).getOrElse(Json.obj())

    val uaEstablisherDetails = establishers.map(_._2).getOrElse(Json.obj())
    val uaTrusteeDetails = trustees.map(_._2).getOrElse(Json.obj())

    (
      Json.obj("items" -> (ifSchemeDetails.as[JsObject] ++ ifEstablishers.as[JsObject] ++ ifTrustee.as[JsObject])),
      uaSchemeDetails.as[JsObject] ++ uaEstablisherDetails ++ uaTrusteeDetails
    )
  }

}