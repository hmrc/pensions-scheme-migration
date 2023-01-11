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

package models

import play.api.mvc.{JavascriptLiteral, PathBindable}

sealed trait MigrationType

case object Scheme extends WithName("scheme") with MigrationType
case object RacDac extends WithName("racdac") with MigrationType

object MigrationType {

  case class UnknownMigrationTypeException() extends Exception

  implicit val jsLiteral: JavascriptLiteral[MigrationType] = {
    case Scheme => "scheme"
    case RacDac => "racDac"
  }

  implicit val isRacDac: MigrationType => Boolean = migType => migType == RacDac

  implicit def modePathBindable(implicit stringBinder: PathBindable[String]): PathBindable[MigrationType] = new
      PathBindable[MigrationType] {

    val modes = Seq(Scheme, RacDac)

    override def bind(key: String, value: String): Either[String, MigrationType] = {
      stringBinder.bind(key, value) match {
        case Right(Scheme.toString) => Right(Scheme)
        case Right(RacDac.toString) => Right(RacDac)
        case _ => Left("MigrationType binding failed")
      }
    }

    override def unbind(key: String, value: MigrationType): String = {
      val modeValue = modes.find(_ == value).map(_.toString).getOrElse(throw UnknownMigrationTypeException())
      stringBinder.unbind(key, modeValue)
    }
  }
}

abstract class WithName(string: String) {
  override val toString: String = string
}

