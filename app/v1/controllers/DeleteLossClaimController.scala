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
import v1.controllers.requestParsers.DeleteLossClaimParser
import v1.models.audit.{AuditEvent, AuditResponse, DeleteLossClaimAuditDetail}
import v1.models.errors._
import v1.models.requestData.DeleteLossClaimRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteLossClaimController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          deleteLossClaimService: DeleteLossClaimService,
                                          deleteLossClaimParser: DeleteLossClaimParser,
                                          auditService: AuditService,
                                          cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "DeleteLossClaimController", endpointName = "Delete a Loss Claim")

  def delete(nino: String, claimId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>

      val rawData = DeleteLossClaimRawData(nino, claimId)
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](deleteLossClaimParser.parseRequest(rawData))
          vendorResponse <- EitherT(deleteLossClaimService.deleteLossClaim(parsedRequest))
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${vendorResponse.correlationId}")

          auditSubmission(DeleteLossClaimAuditDetail(request.userDetails, nino, claimId,
            vendorResponse.correlationId, AuditResponse(NO_CONTENT, Right(None))))


          NoContent
            .withApiHeaders(vendorResponse.correlationId)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result = errorResult(errorWrapper).withApiHeaders(correlationId)

        auditSubmission(DeleteLossClaimAuditDetail(request.userDetails, nino, claimId,
          correlationId, AuditResponse(result.header.status, Left(errorWrapper.auditErrors))))

        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError
           | NinoFormatError
           | ClaimIdFormatError => BadRequest(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: DeleteLossClaimAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext) = {
    val event = AuditEvent("deleteLossClaim", "delete-loss-claim", details)
    auditService.auditEvent(event)
  }
}
