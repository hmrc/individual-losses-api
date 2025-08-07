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

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import shared.config.SharedAppConfig
import shared.controllers.*
import shared.hateoas.HateoasFactory
import shared.services.{EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator
import v4.controllers.validators.RetrieveBFLossValidatorFactory
import v4.models.response.retrieveBFLoss.GetBFLossHateoasData
import v4.services.RetrieveBFLossService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RetrieveBFLossController @Inject() (val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          service: RetrieveBFLossService,
                                          validatorFactory: RetrieveBFLossValidatorFactory,
                                          hateoasFactory: HateoasFactory,
                                          cc: ControllerComponents,
                                          idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: SharedAppConfig)
    extends AuthorisedController(cc) {

  override val endpointName: String = "retrieve-bf-loss"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "RetrieveBFLossController", endpointName = "Retrieve a Brought Forward Loss")

  def retrieve(nino: String, lossId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, lossId)

      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.retrieveBFLoss)
          .withHateoasResult(hateoasFactory)(GetBFLossHateoasData(nino, lossId))

      requestHandler.handleRequest()
    }

}
