/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.bfLosses.create

import config.LossesFeatureSwitches
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import shared.config.SharedAppConfig
import shared.controllers.*
import shared.controllers.validators.Validator
import shared.routing.Version
import shared.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator
import v6.bfLosses.create.model.request.CreateBFLossRequestData
import v6.bfLosses.create.model.response.CreateBFLossResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CreateBFLossController @Inject() (val authService: EnrolmentsAuthService,
                                        val lookupService: MtdIdLookupService,
                                        service: CreateBFLossService,
                                        validatorFactory: CreateBFLossValidatorFactory,
                                        auditService: AuditService,
                                        cc: ControllerComponents,
                                        idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: SharedAppConfig)
    extends AuthorisedController(cc) {

  override val endpointName: String = "create-bf-loss"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateBFLossController", endpointName = "Create a Brought Forward Loss")

  def create(nino: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator: Validator[CreateBFLossRequestData] = validatorFactory.validator(
        nino = nino,
        taxYear = taxYear,
        body = request.body,
        temporalValidationEnabled = LossesFeatureSwitches().isTemporalValidationEnabled
      )

      val requestHandler: RequestHandler.RequestHandlerBuilder[CreateBFLossRequestData, CreateBFLossResponse] =
        RequestHandler
          .withValidator(validator)
          .withService(service.createBFLoss)
          .withPlainJsonResult(CREATED)
          .withAuditing(AuditHandler(
            auditService = auditService,
            auditType = "CreateBroughtForwardLoss",
            transactionName = "create-brought-forward-loss",
            apiVersion = Version(request),
            params = Map("nino" -> nino, "taxYear" -> taxYear),
            requestBody = Some(request.body),
            includeResponse = true
          ))

      requestHandler.handleRequest()
    }

}
