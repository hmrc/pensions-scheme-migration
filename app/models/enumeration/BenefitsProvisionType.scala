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

package models.enumeration

object BenefitsProvisionType extends Enumeration {

  sealed case class TypeValue(name: String, value: String) extends Val(name)

  val opt1 = TypeValue("moneyPurchaseOnly", "Money Purchase benefits only (defined contribution)")
  val opt2 = TypeValue("definedBenefitsOnly", "Defined Benefits only")
  val opt3 = TypeValue("mixedBenefits", "Mixture of money purchase benefits and defined benefits")

  def valueWithName(name: String): String =
    super.withName(name).asInstanceOf[TypeValue].value

  def nameWithValue(value: String): String =
    Seq(opt1, opt2, opt3).find(_.value == value).getOrElse(throw new IllegalArgumentException("Unknown value")).name
}
