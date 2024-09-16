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
import shared.config.AppConfig
import shared.controllers._
import shared.routing.Version
import shared.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator
import v4.controllers.validators.DeleteLossClaimValidatorFactory
import v4.services.DeleteLossClaimService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeleteLossClaimController @Inject() (val authService: EnrolmentsAuthService,
                                           val lookupService: MtdIdLookupService,
                                           service: DeleteLossClaimService,
                                           validatorFactory: DeleteLossClaimValidatorFactory,
                                           auditService: AuditService,
                                           cc: ControllerComponents,
                                           idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends AuthorisedController(cc) {

  override val endpointName: String = "delete-loss-claim"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "DeleteLossClaimController", endpointName = "Delete a Loss Claim")

  def delete(nino: String, claimId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, claimId)

      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.deleteLossClaim)
          .withAuditing(
            AuditHandler(
              auditService,
              auditType = "DeleteLossClaim",
              transactionName = "delete-loss-claim",
              apiVersion = Version(request),
              params = Map("nino" -> nino, "claimId" -> claimId)
            ))

      requestHandler.handleRequest()
    }

}
