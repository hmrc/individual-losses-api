/*
 * Copyright 2022 HM Revenue & Customs
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

import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.errors._
import api.services.MtdIdLookupService
import cats.data.EitherT
import cats.implicits._
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import v3.controllers.requestParsers.DeleteBFLossParser
import v3.models.errors._
import v3.models.request.deleteBFLoss.DeleteBFLossRawData
import v3.services._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteBFLossController @Inject()(val authService: EnrolmentsAuthService,
                                       val lookupService: MtdIdLookupService,
                                       deleteBFLossService: DeleteBFLossService,
                                       deleteBFLossParser: DeleteBFLossParser,
                                       auditService: AuditService,
                                       cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "DeleteBFLossController", endpointName = "Delete a Brought Forward Loss")

  def delete(nino: String, lossId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      val rawData = DeleteBFLossRawData(nino, lossId)
      val result =
        for {
          parsedRequest  <- EitherT.fromEither[Future](deleteBFLossParser.parseRequest(rawData))
          vendorResponse <- EitherT(deleteBFLossService.deleteBFLoss(parsedRequest))
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${vendorResponse.correlationId}")

          auditSubmission(
            GenericAuditDetail(request.userDetails,
                               Map("nino" -> nino, "lossId" -> lossId),
                               None,
                               vendorResponse.correlationId,
                               AuditResponse(httpStatus = NO_CONTENT, response = Right(None)))
          )

          NoContent
            .withApiHeaders(vendorResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result        = errorResult(errorWrapper).withApiHeaders(correlationId)

        auditSubmission(
          GenericAuditDetail(
            request.userDetails,
            Map("nino" -> nino, "lossId" -> lossId),
            None,
            correlationId,
            AuditResponse(httpStatus = result.header.status, response = Left(errorWrapper.auditErrors))
          )
        )

        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError | NinoFormatError | LossIdFormatError => BadRequest(Json.toJson(errorWrapper))
      case RuleDeleteAfterFinalDeclarationError                  => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError                                         => NotFound(Json.toJson(errorWrapper))
      case StandardDownstreamError                               => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: GenericAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event: AuditEvent[GenericAuditDetail] = AuditEvent(
      auditType = "DeleteBroughtForwardLoss",
      transactionName = "delete-brought-forward-loss",
      detail = details
    )
    auditService.auditEvent(event)
  }
}
