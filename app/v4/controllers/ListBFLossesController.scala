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
import v4.controllers.requestParsers.ListBFLossesRequestParser
import v4.models.request.listLossClaims.ListBFLossesRawData
import v4.models.response.listBFLosses.ListBFLossHateoasData
import v4.services.ListBFLossesService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ListBFLossesController @Inject() (val authService: EnrolmentsAuthService,
                                        val lookupService: MtdIdLookupService,
                                        service: ListBFLossesService,
                                        parser: ListBFLossesRequestParser,
                                        hateoasFactory: HateoasFactory,
                                        cc: ControllerComponents,
                                        idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "LisBFLossesController", endpointName = "List Brought Forward Losses")

  def list(nino: String, taxYearBroughtForwardFrom: String, businessId: Option[String], typeOfLoss: Option[String]): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val rawData = ListBFLossesRawData(nino, taxYearBroughtForwardFrom, typeOfLoss, businessId)

      val requestHandler =
        RequestHandler
          .withParser(parser)
          .withService(service.listBFLosses)
          .withResultCreator(ResultCreator.hateoasListWrapping(hateoasFactory)((_, _) => ListBFLossHateoasData(nino)))

      requestHandler.handleRequest(rawData)
    }

}