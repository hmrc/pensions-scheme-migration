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

package utils

import com.typesafe.config.ConfigException
import config.AppConfig
import play.api.Environment
import play.api.libs.json.Json

import javax.inject.{Inject, Singleton}

@Singleton
class CountryOptions @Inject()(environment: Environment, config: AppConfig)  {

 val options = {
      environment.resourceAsStream(config.locationCanonicalList).flatMap {
        in =>
          val locationJsValue = Json.parse(in)
          Json.fromJson[Seq[Seq[String]]](locationJsValue).asOpt.map {
            _.map { countryList =>
              Country( countryList.head, countryList(1))
            }
          }
      }.getOrElse {
        throw new ConfigException.BadValue(config.locationCanonicalList, "country json does not exist")
      }
  }

  def getCountryCodeFromName(name: String): Option[String] = {
    options
      .find(_.name == name)
      .map(_.code)
  }

}

object CountryOptions {

  def getCountries(environment: Environment, fileName: String): Seq[Country] = {
    environment.resourceAsStream(fileName).flatMap {
      in =>
        val locationJsValue = Json.parse(in)
        Json.fromJson[Seq[Seq[String]]](locationJsValue).asOpt.map {
          _.map { countryList =>
            Country(countryList(1).replaceAll("country:", ""), countryList.head)
          }
        }
    }.getOrElse {
      throw new ConfigException.BadValue(fileName, "country json does not exist")
    }
  }

  def getCountryCodes(environment: Environment, fileName: String) : Seq[String] = {
    environment.resourceAsStream(fileName).map { in =>
      val locationJsValue = Json.parse(in)
      Json.fromJson[Seq[Seq[String]]](locationJsValue).asOpt.map {
        _.map { countryList =>
          countryList(1).replaceAll("country:", "")
        }
      }.fold[Seq[String]](List.empty)(identity)
    }.getOrElse {
      throw new ConfigException.BadValue(fileName, "country json does not exist")
    }
  }
}

