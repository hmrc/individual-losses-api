/*
 * Copyright 2020 HM Revenue & Customs
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

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import v1.controllers.requestParsers.DeleteBFLossParser
import v1.models.audit.{AuditEvent, AuditResponse, DeleteBFLossAuditDetail}
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

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "DeleteBFLossController", endpointName = "Delete a Brought Forward Loss")

  def delete(nino: String, lossId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>

      val rawData = DeleteBFLossRawData(nino, lossId)
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](deleteBFLossParser.parseRequest(rawData))
          vendorResponse <- EitherT(deleteBFLossService.deleteBFLoss(parsedRequest))
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${vendorResponse.correlationId}")

          auditSubmission(DeleteBFLossAuditDetail(request.userDetails, nino, lossId,
            vendorResponse.correlationId, AuditResponse(NO_CONTENT, None, None)))

          NoContent
            .withApiHeaders(vendorResponse.correlationId)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result = errorResult(errorWrapper).withApiHeaders(correlationId)

        auditSubmission(DeleteBFLossAuditDetail(request.userDetails, nino, lossId,
          correlationId, AuditResponse(result.header.status, Left(errorWrapper.auditErrors))))

        result
      }.merge
    }


  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError
           | NinoFormatError
           | LossIdFormatError => BadRequest(Json.toJson(errorWrapper))
      case RuleDeleteAfterCrystallisationError => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: DeleteBFLossAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext) = {
    val event = AuditEvent("deleteBroughtForwardLoss", "delete-brought-forward-Loss", details)
    auditService.auditEvent(event)
  }
}
