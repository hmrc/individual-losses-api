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

import api.controllers._
import api.hateoas.HateoasFactory
import api.services.{EnrolmentsAuthService, MtdIdLookupService}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.IdGenerator
import v4.controllers.requestParsers.ListLossClaimsRequestParser
import v4.models.request.listLossClaims.ListLossClaimsRawData
import v4.models.response.listLossClaims.ListLossClaimsHateoasData
import v4.services.ListLossClaimsService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ListLossClaimsController @Inject() (val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          service: ListLossClaimsService,
                                          parser: ListLossClaimsRequestParser,
                                          hateoasFactory: HateoasFactory,
                                          cc: ControllerComponents,
                                          idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "ListLossClaimsController", endpointName = "List Loss Claims")

  def list(nino: String, taxYear: String, typeOfLoss: Option[String], businessId: Option[String], typeOfClaim: Option[String]): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val rawData =
        ListLossClaimsRawData(nino, taxYearClaimedFor = taxYear, typeOfLoss = typeOfLoss, businessId = businessId, typeOfClaim = typeOfClaim)

      val requestHandler =
        RequestHandlerOld
          .withParser(parser)
          .withService(service.listLossClaims)
          .withResultCreator(ResultCreatorOld.hateoasListWrapping(hateoasFactory)((_, _) =>
            ListLossClaimsHateoasData(nino, taxYearClaimedFor = taxYear)))

      requestHandler.handleRequest(rawData)
    }

}
