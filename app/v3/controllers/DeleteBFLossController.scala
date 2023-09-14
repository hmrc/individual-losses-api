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
import api.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.IdGenerator
import v3.controllers.requestParsers.DeleteBFLossRequestParser
import v3.models.request.deleteBFLosses.DeleteBFLossRawData
import v3.services.DeleteBFLossService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeleteBFLossController @Inject() (val authService: EnrolmentsAuthService,
                                        val lookupService: MtdIdLookupService,
                                        service: DeleteBFLossService,
                                        parser: DeleteBFLossRequestParser,
                                        auditService: AuditService,
                                        cc: ControllerComponents,
                                        idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "DeleteBFLossController", endpointName = "Delete a Brought Forward Loss")

  def delete(nino: String, lossId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val rawData = DeleteBFLossRawData(nino, lossId)

      val requestHandler =
        RequestHandlerOld
          .withParser(parser)
          .withService(service.deleteBFLoss)
          .withAuditing(
            AuditHandlerOld(
              auditService,
              auditType = "DeleteBroughtForwardLoss",
              transactionName = "delete-brought-forward-loss",
              params = Map("nino" -> nino, "lossId" -> lossId)
            ))

      requestHandler.handleRequest(rawData)
    }

}
