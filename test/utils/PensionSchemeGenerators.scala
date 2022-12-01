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

package utils

import org.joda.time.LocalDate
import org.scalacheck.Gen
import play.api.libs.json.{JsObject, Json}

trait PensionSchemeGenerators { // scalastyle:off magic.number
  val specialCharStringGen: Gen[String] = Gen.listOfN[Char](160, Gen.alphaChar).map(_.mkString)
  val addressLineGen: Gen[String] = Gen.listOfN[Char](35, Gen.alphaChar).map(_.mkString)
  val addressLineOptional: Gen[Option[String]] = Gen.option(addressLineGen)
  val postalCodeGem: Gen[String] = Gen.listOfN[Char](10, Gen.alphaChar).map(_.mkString)
  val optionalNinoGenerator: Gen[Option[String]] = Gen.option("SL221122D")
  val ninoGenerator: Gen[String] = Gen.const("SL221122D")
  val crnGenerator: Gen[String] = Gen.const("11111111")
  val vatGenerator: Gen[String] = Gen.const("123456789")
  val payeGenerator: Gen[String] = Gen.const("1111111111111")
  val utrGeneratorFromUser: Gen[String] = {
    val utrRange: Gen[String] = Gen.listOfN[Char](randomNumberFromRange(10, 13), Gen.numChar).map(_.mkString)
    randomNumberFromRange(1, 3) match {
      case 1 => utrRange
      case 2 => "k" + utrRange
      case 3 => utrRange.toString + "k"
    }
  }
  val optionalPostalCodeGen: Gen[Option[String]] = Gen.option(Gen.listOfN[Char](10, Gen.alphaChar).map(_.mkString))
  val countryName: Gen[String] = Gen.const("Netherlands")
  val countryCode: Gen[String] = Gen.const("NL")


  def randomNumberFromRange(min: Int, max: Int): Int = Gen.chooseNum(min, max).sample.fold(min)(c => c)

  val nameGenerator: Gen[String] = Gen.listOfN[Char](randomNumberFromRange(1, 35), Gen.alphaChar).map(_.mkString)
  val dateGenerator: Gen[LocalDate] = for {
    day <- Gen.choose(1, 28)
    month <- Gen.choose(1, 12)
    year <- Gen.choose(1990, 2000)
  } yield new LocalDate(year, month, day)
  val reasonGen: Gen[String] = Gen.listOfN[Char](randomNumberFromRange(1, 160), Gen.alphaChar).map(_.mkString)

  val booleanGen: Gen[Boolean] = Gen.oneOf(true, false)

  val policyNumberGen: Gen[String] = Gen.listOfN[Char](55, Gen.alphaChar).map(_.mkString)
  val otherSchemeStructureGen: Gen[String] = Gen.listOfN[Char](160, Gen.alphaChar).map(_.mkString)

  val boolenGen: Gen[Boolean] = Gen.oneOf(Seq(true, false))

  val schemeStatusGen: Gen[String] = Gen.oneOf(
    Seq("Open", "Pending", "Pending Info Required", "Pending Info Received", "Deregistered", "Wound-up", "Rejected Under Appeal")
  )

  val memberGen: Gen[String] = Gen.oneOf(Seq("0",
    "1",
    "2-11",
    "12-50",
    "51-10000",
    "10001+"))

  val schemeTypeGen: Gen[Option[String]] = Gen.option(Gen.oneOf(Seq("single",
    "grouplife-deathinservice",
    "body-corporate",
    "Other")))

  protected def optional(key: String, element: Option[String]): JsObject = {
    element.map { value =>
      Json.obj(key -> value)
    }.getOrElse(Json.obj())
  }

  protected def optionalWithReason(key: String, element: Option[String], reason: String): JsObject = {
    element.map { value =>
      Json.obj(key -> value)
    }.getOrElse(Json.obj(reason -> reason))
  }

  protected def crnJsValue(crn: Option[String], wrapper: String): JsObject =
    crn.fold(Json.obj("haveCompanyNumber" -> false, "noCompanyNumberReason" -> "noCrnReason"))(crn =>
      Json.obj("haveCompanyNumber" -> true, wrapper -> Json.obj("value" -> crn)))

  protected def ninoJsValue(nino: Option[String], wrapper: String): JsObject =
    nino.fold(Json.obj("hasNino" -> false, "noNinoReason" -> "noNinoReason"))(nino =>
      Json.obj("hasNino" -> true, wrapper -> Json.obj("value" -> nino)))

  protected def vatJsValue(vat: Option[String], wrapper: String): JsObject =
    vat.fold(Json.obj("haveVat" -> false))(vat =>
      Json.obj("haveVat" -> true, wrapper -> Json.obj("value" -> vat)))

  protected def payeJsValue(paye: Option[String], wrapper: String): JsObject =
    paye.fold(Json.obj("havePaye" -> false))(paye =>
      Json.obj("havePaye" -> true, wrapper -> Json.obj("value" -> paye)))

}
