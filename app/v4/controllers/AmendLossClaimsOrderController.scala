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

import shared.controllers._
import shared.hateoas.HateoasFactory
import shared.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import shared.config.AppConfig
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import shared.routing.{Version, Version4}
import shared.utils.IdGenerator
import v4.controllers.validators.AmendLossClaimsOrderValidatorFactory
import v4.models.response.amendLossClaimsOrder.AmendLossClaimsOrderHateoasData
import v4.services.AmendLossClaimsOrderService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AmendLossClaimsOrderController @Inject() (val authService: EnrolmentsAuthService,
                                                val lookupService: MtdIdLookupService,
                                                service: AmendLossClaimsOrderService,
                                                validatorFactory: AmendLossClaimsOrderValidatorFactory,
                                                hateoasFactory: HateoasFactory,
                                                auditService: AuditService,
                                                cc: ControllerComponents,
                                                idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "AmendLossClaimsOrderController", endpointName = "Amend a Loss Claim Order")

  def amendClaimsOrder(nino: String, taxYearClaimedFor: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val apiVersion: Version = Version.from(request, orElse = Version4)
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, taxYearClaimedFor, request.body)

      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.amendLossClaimsOrder)
          .withHateoasResult(hateoasFactory)(AmendLossClaimsOrderHateoasData(nino, taxYearClaimedFor))
          .withAuditing(AuditHandler(
            auditService,
            auditType = "AmendLossClaimOrder",
            transactionName = "amend-loss-claim-order",
            apiVersion = Version.from(request, orElse = Version4),
            params = Map("nino" -> nino, "taxYearClaimedFor" -> taxYearClaimedFor),
            requestBody = Some(request.body),
            includeResponse = true
          ))

      requestHandler.handleRequest()
    }

}
