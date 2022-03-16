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

package models.enumeration

object SchemeMembers extends Enumeration {

  sealed case class TypeValue(name: String, value: String,tpssValue:String) extends Val(name)

  val opt1 = TypeValue("opt1", "0","0")
  val opt2 = TypeValue("opt2", "1","1")
  val opt3 = TypeValue("opt3", "2 to 11","2-11")
  val opt4 = TypeValue("opt4", "12 to 50","12-50")
  val opt5 = TypeValue("opt5", "51 to 10,000","51-10000")
  val opt6 = TypeValue("opt6", "More than 10,000","10001+")

  def valueWithName(name: String): String = {
    super.withName(name).asInstanceOf[TypeValue].value
  }

  def nameWithValue(value: String): Option[String] =
    Seq(opt1, opt2, opt3, opt4, opt5, opt6).find(_.value == value).map(_.name)

  def tppsNameWithValue(value: String): Option[String] =
    Seq(opt1, opt2, opt3, opt4, opt5, opt6).find(_.tpssValue == value).map(_.name)
}
