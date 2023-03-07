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

package api.endpoints.bfLoss.list.v4

import api.controllers._
import api.endpoints.bfLoss.list.v4.request.{ ListBFLossesParser, ListBFLossesRawData }
import api.endpoints.bfLoss.list.v4.response.ListBFLossHateoasData
import api.hateoas.HateoasFactory
import api.services.{ EnrolmentsAuthService, MtdIdLookupService }
import play.api.mvc.{ Action, AnyContent, ControllerComponents }
import utils.{ IdGenerator, Logging }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class ListBFLossesController @Inject() (val authService: EnrolmentsAuthService,
                                        val lookupService: MtdIdLookupService,
                                        service: ListBFLossesService,
                                        parser: ListBFLossesParser,
                                        hateoasFactory: HateoasFactory,
                                        cc: ControllerComponents,
                                        idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with Logging {

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
