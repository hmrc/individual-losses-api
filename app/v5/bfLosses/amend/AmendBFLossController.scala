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

package v5.bfLosses.amend

import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import shared.config.AppConfig
import shared.controllers._
import shared.routing.Version
import shared.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator
import v5.bfLosses.amend

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AmendBFLossController @Inject() (val authService: EnrolmentsAuthService,
                                       val lookupService: MtdIdLookupService,
                                       service: amend.AmendBFLossService,
                                       validatorFactory: AmendBFLossValidatorFactory,
                                       auditService: AuditService,
                                       cc: ControllerComponents,
                                       idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends AuthorisedController(cc) {

  override val endpointName: String = "amend-bf-loss"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "AmendBFLossController", endpointName = "Amend a Brought Forward Loss Amount")

  def amend(nino: String, lossId: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, lossId, request.body)

      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.amendBFLoss)
          .withPlainJsonResult()
          .withAuditing(AuditHandler(
            auditService,
            auditType = "AmendBroughtForwardLoss",
            transactionName = "amend-brought-forward-loss",
            apiVersion = Version(request),
            params = Map("nino" -> nino, "lossId" -> lossId),
            requestBody = Some(request.body),
            includeResponse = true
          ))

      requestHandler.handleRequest()
    }

}
