/*
 * Copyright 2022 HM Revenue & Customs
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

package api.endpoints.lossClaim.amendOrder.v3

import api.controllers._
import api.endpoints.lossClaim.amendOrder.v3.request.{ AmendLossClaimsOrderParser, AmendLossClaimsOrderRawData }
import api.endpoints.lossClaim.amendOrder.v3.response.AmendLossClaimsOrderHateoasData
import api.hateoas.HateoasFactory
import api.services.{ AuditService, EnrolmentsAuthService, MtdIdLookupService }
import play.api.libs.json.JsValue
import play.api.mvc.{ Action, AnyContentAsJson, ControllerComponents }
import utils.{ IdGenerator, Logging }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class AmendLossClaimsOrderController @Inject() (val authService: EnrolmentsAuthService,
                                                val lookupService: MtdIdLookupService,
                                                service: AmendLossClaimsOrderService,
                                                parser: AmendLossClaimsOrderParser,
                                                hateoasFactory: HateoasFactory,
                                                auditService: AuditService,
                                                cc: ControllerComponents,
                                                idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with Logging {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "AmendLossClaimsOrderController", endpointName = "Amend a Loss Claim Order")

  def amendClaimsOrder(nino: String, taxYearClaimedFor: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val rawData = AmendLossClaimsOrderRawData(nino, taxYearClaimedFor, AnyContentAsJson(request.body))

      val requestHandler =
        RequestHandler
          .withParser(parser)
          .withService(service.amendLossClaimsOrder)
          .withHateoasResult(hateoasFactory)(AmendLossClaimsOrderHateoasData(nino))
          .withAuditing(AuditHandler(
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
