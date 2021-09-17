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

package config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  lazy val appName: String = config.get[String]("appName")
  val authBaseUrl: String = servicesConfig.baseUrl("auth")
  lazy val ifURL: String = servicesConfig.baseUrl(serviceName = "if-hod")

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String     = config.get[String]("microservice.metrics.graphite.host")

  lazy val authorization: String = "Bearer " + config.getOptional[String]("microservice.services.des-hod.authorizationToken").getOrElse("local")

  lazy val integrationframeworkEnvironment: String = config.getOptional[String](
    path = "microservice.services.if-hod.env").getOrElse("local")
  lazy val integrationframeworkAuthorization: String = "Bearer " + config.getOptional[String](
    path = "microservice.services.if-hod.authorizationToken").getOrElse("local")
  lazy val listOfSchemesUrl: String = s"$ifURL${config.get[String]("serviceUrls.if.list.of.schemes")}"
  lazy val schemeRegistrationIFUrl: String = s"$ifURL${config.get[String]("serviceUrls.if.scheme.register")}"
  lazy val racDacStubUrl: String = s"$ifURL${config.get[String]("serviceUrls.if.racDac.stub")}"
}
