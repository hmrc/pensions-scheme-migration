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

package connector

import com.google.inject.Inject
import config.AppConfig
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID.randomUUID

class HeaderUtils @Inject()(config: AppConfig) {

  private val logger = Logger(classOf[HeaderUtils])

  def integrationFrameworkHeader(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val requestId = getCorrelationId(None)

    Seq("Environment" -> config.integrationframeworkEnvironment,
      "Authorization" -> config.integrationframeworkAuthorization,
      "Content-Type" -> "application/json",
      "CorrelationId" -> requestId)
  }

  private def getCorrelationId(requestId: Option[String]): String = {
    requestId.getOrElse {
      logger.error("No Request Id found")
      randomUUID.toString
    }.replaceAll("(govuk-tax-)", "").slice(0, 36)
  }
}
