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
import shared.services.{EnrolmentsAuthService, MtdIdLookupService}
import shared.config.AppConfig
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import shared.routing.{Version, Version4}
import shared.utils.IdGenerator
import v4.controllers.validators.RetrieveLossClaimValidatorFactory
import v4.models.response.retrieveLossClaim.GetLossClaimHateoasData
import v4.services.RetrieveLossClaimService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RetrieveLossClaimController @Inject() (val authService: EnrolmentsAuthService,
                                             val lookupService: MtdIdLookupService,
                                             service: RetrieveLossClaimService,
                                             validatorFactory: RetrieveLossClaimValidatorFactory,
                                             hateoasFactory: HateoasFactory,
                                             cc: ControllerComponents,
                                             idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "RetrieveLossClaimController", endpointName = "Retrieve a Loss Claim")

  def retrieve(nino: String, claimId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val apiVersion: Version = Version.from(request, orElse = Version4)
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, claimId)

      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.retrieveLossClaim)
          .withHateoasResult(hateoasFactory)(GetLossClaimHateoasData(nino, claimId))

      requestHandler.handleRequest()
    }

}
