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

package v5.bfLossClaims.create

import api.controllers._
import api.hateoas.HateoasFactory
import api.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import config.AppConfig
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import routing.{Version, Version4, Version5}
import utils.IdGenerator
import v5.bfLossClaims.create.def1.model.response.CreateBFLossHateoasData
import v5.bfLossClaims.create.def1.model.response.Def1_CreateBFLossResponse.LinksFactory

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CreateBFLossController @Inject()(val authService: EnrolmentsAuthService,
                                       val lookupService: MtdIdLookupService,
                                       service: CreateBFLossService,
                                       validatorFactory: CreateBFLossValidatorFactory,
                                       hateoasFactory: HateoasFactory,
                                       auditService: AuditService,
                                       cc: ControllerComponents,
                                       idGenerator: IdGenerator)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateBFLossController", endpointName = "Create a Brought Forward Loss")

  def create(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val apiVersion: Version = Version.from(request, orElse = Version5)
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, request.body)

      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.createBFLoss)
          .withHateoasResultFrom(hateoasFactory)(
            (_, responseData) => CreateBFLossHateoasData(nino, responseData.lossId),
            successStatus = CREATED
          )
          .withAuditing(AuditHandler(
            auditService,
            auditType = "CreateBroughtForwardLoss",
            transactionName = "create-brought-forward-loss",
            apiVersion = Version.from(request, orElse = Version4),
            params = Map("nino" -> nino),
            requestBody = Some(request.body),
            includeResponse = true
          ))

      requestHandler.handleRequest()
    }

}
