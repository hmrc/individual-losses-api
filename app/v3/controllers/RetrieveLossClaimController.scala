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
import api.services.{EnrolmentsAuthService, MtdIdLookupService}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.IdGenerator
import v3.controllers.requestParsers.RetrieveLossClaimRequestParser
import v3.models.request.retrieveLossClaim.RetrieveLossClaimRawData
import v3.models.response.retrieveLossClaim.GetLossClaimHateoasData
import v3.services.RetrieveLossClaimService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RetrieveLossClaimController @Inject() (val authService: EnrolmentsAuthService,
                                             val lookupService: MtdIdLookupService,
                                             service: RetrieveLossClaimService,
                                             parser: RetrieveLossClaimRequestParser,
                                             hateoasFactory: HateoasFactory,
                                             cc: ControllerComponents,
                                             idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "RetrieveLossClaimController", endpointName = "Retrieve a Loss Claim")

  def retrieve(nino: String, claimId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val rawData = RetrieveLossClaimRawData(nino, claimId)

      val requestHandler =
        RequestHandlerOld
          .withParser(parser)
          .withService(service.retrieveLossClaim)
          .withHateoasResult(hateoasFactory)(GetLossClaimHateoasData(nino, claimId))

      requestHandler.handleRequest(rawData)
    }

}
