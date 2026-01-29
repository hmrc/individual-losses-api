/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.delete

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import shared.config.SharedAppConfig
import shared.controllers.*
import shared.routing.Version
import shared.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeleteLossesAndClaimsController @Inject() (val authService: EnrolmentsAuthService,
                                                 val lookupService: MtdIdLookupService,
                                                 service: DeleteLossesAndClaimsService,
                                                 validatorFactory: DeleteLossesAndClaimsValidatorFactory,
                                                 auditService: AuditService,
                                                 cc: ControllerComponents,
                                                 idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: SharedAppConfig)
    extends AuthorisedController(cc) {
  override val endpointName: String = "delete-losses-and-claims"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "DeleteLossesAndClaimsController", endpointName = "Delete Losses and Claims")

  def delete(nino: String, businessId: String, taxYear: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, businessId, taxYear)

      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.deleteLossClaimsService)
          .withAuditing(AuditHandler(
            auditService,
            auditType = "DeleteLossesAndClaims",
            transactionName = "delete-losses-and-claims",
            apiVersion = Version(request),
            params = Map("nino" -> nino, "businessId" -> businessId, "taxYear" -> taxYear)
          ))

      requestHandler.handleRequest()
    }

}
