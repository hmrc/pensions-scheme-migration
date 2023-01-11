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

object SchemeType extends Enumeration {

  sealed case class TypeValue(name: String, value: String,etmpValue:String) extends Val(name)

  val single = TypeValue("single", "single"
    ,"A single trust under which all of the assets are held for the benefit of all members of the scheme")
  val group = TypeValue("group", "grouplife-deathinservice","A group life/death in service scheme")
  val corp = TypeValue("corp", "body-corporate","A body corporate")
  val other = TypeValue("other", "Other","Other")

  def valueWithName(name: String): String = {
    super.withName(name).asInstanceOf[TypeValue].value
  }

  def nameWithValue(value: String): String =
    Seq(single, group, corp, other).find(_.value == value).getOrElse(other).name

  def etmpValueWithName(name: String): String = {
    super.withName(name).asInstanceOf[TypeValue].etmpValue
  }

  def nameWithEtmpValue(etmpValue: String): String =
    Seq(single, group, corp, other).find(_.etmpValue == etmpValue).getOrElse(other).name
}
