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

package v3.controllers

import api.controllers._
import api.hateoas.HateoasFactory
import api.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import utils.IdGenerator
import v3.controllers.requestParsers.AmendLossClaimsOrderRequestParser
import v3.models.request.amendLossClaimsOrder.AmendLossClaimsOrderRawData
import v3.models.response.amendLossClaimsOrder.AmendLossClaimsOrderHateoasData
import v3.services.AmendLossClaimsOrderService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AmendLossClaimsOrderController @Inject() (val authService: EnrolmentsAuthService,
                                                val lookupService: MtdIdLookupService,
                                                service: AmendLossClaimsOrderService,
                                                parser: AmendLossClaimsOrderRequestParser,
                                                hateoasFactory: HateoasFactory,
                                                auditService: AuditService,
                                                cc: ControllerComponents,
                                                idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "AmendLossClaimsOrderController", endpointName = "Amend a Loss Claim Order")

  def amendClaimsOrder(nino: String, taxYearClaimedFor: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val rawData = AmendLossClaimsOrderRawData(nino, taxYearClaimedFor, AnyContentAsJson(request.body))

      val requestHandler =
        RequestHandlerOld
          .withParser(parser)
          .withService(service.amendLossClaimsOrder)
          .withHateoasResult(hateoasFactory)(AmendLossClaimsOrderHateoasData(nino, taxYearClaimedFor))
          .withAuditing(AuditHandlerOld(
            auditService,
            auditType = "AmendLossClaimOrder",
            transactionName = "amend-loss-claim-order",
            params = Map("nino" -> nino, "taxYearClaimedFor" -> taxYearClaimedFor),
            requestBody = Some(request.body),
            includeResponse = true
          ))

      requestHandler.handleRequest(rawData)
    }

}
