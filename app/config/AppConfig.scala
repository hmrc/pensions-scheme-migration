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

package config

import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig, env: Environment) {

  private def loadConfig(key: String): String = config.getOptional[String](key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  lazy val appName: String = config.get[String]("appName")
  lazy val ifURL: String = servicesConfig.baseUrl(serviceName = "if-hod")

  lazy val baseUrlPensionsSchemeMigration: String = servicesConfig.baseUrl("pensions-scheme-migration")

  lazy val integrationframeworkEnvironment: String = config.getOptional[String](
    path = "microservice.services.if-hod.env").getOrElse("local")
  lazy val integrationframeworkAuthorization: String = "Bearer " + config.getOptional[String](
    path = "microservice.services.if-hod.authorizationToken").getOrElse("local")
  lazy val listOfSchemesUrl: String = s"$ifURL${config.get[String]("serviceUrls.if.list.of.schemes")}"
  lazy val legacySchemeDetailsUrl: String = s"$ifURL${config.get[String]("serviceUrls.if.legacy.scheme.details")}"
  lazy val schemeRegistrationIFUrl: String = s"$ifURL${config.get[String]("serviceUrls.if.scheme.register")}"
  lazy val locationCanonicalList: String = loadConfig("location.canonical.list.all")
  lazy val pensionsAdministratorUrl = s"${servicesConfig.baseUrl("pension-administrator")}"
  lazy val bulkMigrationConfirmationEmailTemplateId: String = loadConfig("email.bulkMigrationConfirmationTemplateId")
  lazy val emailApiUrl: String = s"${servicesConfig.baseUrl("email")}"
  lazy val emailSendForce: Boolean = config.getOptional[Boolean]("email.force").getOrElse(false)
  lazy val getPSAMinDetails: String = s"$pensionsAdministratorUrl${config.get[String]("urls.get-psa-min-details")}"

  val mongoEncryptionKey: Option[String] = config.getOptional[String]("mongodb.encryption.key") match {
    case None if env.mode == Mode.Prod => throw new RuntimeException("Encryption key is not set")
    case x => x
  }
}
