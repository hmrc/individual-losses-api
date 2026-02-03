/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.createAmend

import config.LossesFeatureSwitches
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import shared.config.SharedAppConfig
import shared.controllers.*
import shared.controllers.validators.Validator
import shared.routing.Version
import shared.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator
import v7.lossesAndClaims.createAmend.request.CreateAmendLossesAndClaimsRequestData

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CreateAmendLossesAndClaimsController @Inject() (val authService: EnrolmentsAuthService,
                                                      val lookupService: MtdIdLookupService,
                                                      service: CreateAmendLossesAndClaimsService,
                                                      validatorFactory: CreateAmendLossesAndClaimsValidationFactory,
                                                      auditService: AuditService,
                                                      cc: ControllerComponents,
                                                      idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: SharedAppConfig)
    extends AuthorisedController(cc) {

  override val endpointName: String = "create-and-amend-losses-and-claims"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateAmendLossesAndClaimsController", endpointName = " Create and Amend Losses And Claims")

  def createAmend(nino: String, businessId: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator: Validator[CreateAmendLossesAndClaimsRequestData] = validatorFactory.validator(
        nino = nino,
        businessId = businessId,
        taxYear = taxYear,
        body = request.body,
        temporalValidationEnabled = LossesFeatureSwitches().isTemporalValidationEnabled
      )
      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.createAmendLossesAndClaims)
          .withNoContentResult()
          .withAuditing(AuditHandler(
            auditService,
            auditType = "CreateAmendLossesAndClaims",
            transactionName = "create-and-amend-losses-and-claims",
            apiVersion = Version(request),
            params = Map("nino" -> nino, "businessId" -> businessId, "taxYear" -> taxYear),
            requestBody = Option(request.body),
            includeResponse = true
          ))

      requestHandler.handleRequest()
    }

}
