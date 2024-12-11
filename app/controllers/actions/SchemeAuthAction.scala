/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.actions

import connector.utils.HttpResponseHelper
import play.api.Logging
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionFunction, Result}
import service.PensionSchemeService
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SchemeActionImpl (pstr: String, schemeService: PensionSchemeService)
                          (implicit val executionContext: ExecutionContext)
  extends ActionFunction[AuthRequest, AuthRequest] with BackendHeaderCarrierProvider with Logging with HttpResponseHelper {


  override def invokeBlock[A](request: AuthRequest[A], block: AuthRequest[A] => Future[Result]): Future[Result] = {

    val isAssociated = schemeService.isAssociated(
      PsaId(request.psaId),
      pstr
    )(request)

    isAssociated.flatMap {
      case true => block(request)
      case false =>
        logger.warn("Potentially prevented unauthorised access")
        Future.successful(Forbidden("PSA is not associated with pension scheme"))
    }
  }
}




class SchemeAuthAction @Inject()(schemeService: PensionSchemeService)(implicit ec: ExecutionContext){
  def apply(pstr: String): ActionFunction[AuthRequest, AuthRequest] =
    new SchemeActionImpl(pstr, schemeService)
}
