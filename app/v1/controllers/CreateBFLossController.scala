/*
 * Copyright 2019 HM Revenue & Customs
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

package v1.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import v1.controllers.requestParsers.CreateBFLossParser
import v1.models.requestData.CreateBFLossRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateBFLossController @Inject()(val authService: EnrolmentsAuthService,
                                       val lookupService: MtdIdLookupService,
                                       createBFLossService: CreateBFLossService,
                                       createBFLossParser: CreateBFLossParser,
                                       auditService: AuditService,
                                       cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {
  protected val logger: Logger = Logger(this.getClass)

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateBFLossController", endpointName = "createBroughtForwardLoss")

  def create(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      createBFLossParser.parseRequest(CreateBFLossRawData(nino, AnyContentAsJson(request.body)))
    }
}
