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

package connector

import com.google.inject.{Inject, Singleton}
import config.AppConfig
import models.SendEmailRequest
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}


sealed trait EmailStatus

case object EmailSent extends EmailStatus

case object EmailNotSent extends EmailStatus


@Singleton
class EmailConnector @Inject()(http: HttpClientV2, config: AppConfig) extends Logging {

  lazy val postUrl: String = s"${config.emailApiUrl}/hmrc/email"

  def sendEmail(emailAddress: String, templateName: String, params: Map[String, String], callbackUrl: String)
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailStatus] = {
    val sendEmailReq = SendEmailRequest(
      to = List(emailAddress),
      templateId = templateName,
      parameters = params,
      force = config.emailSendForce,
      eventUrl = callbackUrl
    )
    val jsonData = Json.toJson(sendEmailReq)
    logger.debug(s"Data to email: $jsonData for email address $emailAddress")

    http.post(url"$postUrl").withBody(jsonData).execute[HttpResponse].map { response =>
      response.status match {
        case ACCEPTED =>
          logger.info("Email sent successfully.")
          EmailSent
        case status =>
          logger.warn(s"Email not sent. Failure with response status $status")
          EmailNotSent
      }
    } recoverWith logExceptions
  }


  private def logExceptions: PartialFunction[Throwable, Future[EmailStatus]] = {
    case t: Throwable =>
      logger.warn("Unable to connect to Email Service", t)
      Future.successful(EmailNotSent)
  }
}
