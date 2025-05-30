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

package v4.controllers

import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import shared.config.SharedAppConfig
import shared.controllers._
import shared.hateoas.HateoasFactory
import shared.routing.Version
import shared.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator
import v4.controllers.validators.AmendLossClaimTypeValidatorFactory
import v4.models.response.amendLossClaimType.AmendLossClaimTypeHateoasData
import v4.services.AmendLossClaimTypeService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AmendLossClaimTypeController @Inject() (val authService: EnrolmentsAuthService,
                                              val lookupService: MtdIdLookupService,
                                              service: AmendLossClaimTypeService,
                                              validatorFactory: AmendLossClaimTypeValidatorFactory,
                                              hateoasFactory: HateoasFactory,
                                              auditService: AuditService,
                                              cc: ControllerComponents,
                                              idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: SharedAppConfig)
    extends AuthorisedController(cc) {

  override val endpointName: String = "amend-loss-claim-type"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "AmendLossClaimTypeController", endpointName = "Amend a Loss Claim Type")

  def amend(nino: String, claimId: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, claimId, request.body)

      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.amendLossClaimType)
          .withHateoasResult(hateoasFactory)(AmendLossClaimTypeHateoasData(nino, claimId))
          .withAuditing(AuditHandler(
            auditService,
            auditType = "AmendLossClaim",
            transactionName = "amend-loss-claim",
            apiVersion = Version(request),
            params = Map("nino" -> nino, "claimId" -> claimId),
            requestBody = Some(request.body),
            includeResponse = true
          ))

      requestHandler.handleRequest()
    }

}
