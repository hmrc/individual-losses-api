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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import v1.controllers.requestParsers.DeleteBFLossParser
import v1.models.errors._
import v1.models.requestData.DeleteBFLossRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteBFLossController @Inject()(val authService: EnrolmentsAuthService,
                                       val lookupService: MtdIdLookupService,
                                       deleteBFLossService: DeleteBFLossService,
                                       deleteBFLossParser: DeleteBFLossParser,
                                       auditService: AuditService,
                                       cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController {


  def delete(nino: String, lossId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>

      deleteBFLossParser.parseRequest(DeleteBFLossRawData(nino, lossId)) match {
        case Right(deleteBFLossRequest) =>
          deleteBFLossService.deleteBFLoss(deleteBFLossRequest).map {
            case Right(desResponse) =>
              logger.info(s"[DeleteBFLossController] Success response received with correlationId: ${desResponse.correlationId}")
              NoContent
                .withApiHeaders(desResponse.correlationId)

            case Left(errorWrapper) =>
              val result = processError(errorWrapper).withApiHeaders(getCorrelationId(errorWrapper))
              result
          }
        case Left(errorWrapper) =>
          val result = processError(errorWrapper).withApiHeaders(getCorrelationId(errorWrapper))
          Future.successful(result)
      }
    }

  private def processError(errorWrapper: ErrorWrapper) = {
    errorWrapper.error match {
      case BadRequestError
           | NinoFormatError
           | LossIdFormatError => BadRequest(Json.toJson(errorWrapper))
      case RuleDeleteAfterCrystallisationError => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

}
