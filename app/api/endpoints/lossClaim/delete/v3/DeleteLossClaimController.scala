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

package api.endpoints.lossClaim.delete.v3

import api.controllers.{AuthorisedController, BaseController, EndpointLogContext}
import api.endpoints.lossClaim.delete.v3.request.{DeleteLossClaimParser, DeleteLossClaimRawData}
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import cats.data.EitherT
import cats.implicits._
import play.api.http.MimeTypes
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.IdGenerator

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteLossClaimController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          deleteLossClaimService: DeleteLossClaimService,
                                          deleteLossClaimParser: DeleteLossClaimParser,
                                          auditService: AuditService,
                                          cc: ControllerComponents,
                                          idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "DeleteLossClaimController", endpointName = "Delete a Loss Claim")

  def delete(nino: String, claimId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val correlationId: String = idGenerator.getCorrelationId

      val rawData = DeleteLossClaimRawData(nino, claimId)

      val result =
        for {
          parsedRequest  <- EitherT.fromEither[Future](deleteLossClaimParser.parseRequest(rawData))
          vendorResponse <- EitherT(deleteLossClaimService.deleteLossClaim(parsedRequest))
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${vendorResponse.correlationId}")

          auditSubmission(
            GenericAuditDetail(request.userDetails,
                               Map("nino" -> nino, "claimId" -> claimId),
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
            Map("nino" -> nino, "claimId" -> claimId),
            None,
            correlationId,
            AuditResponse(httpStatus = result.header.status, response = Left(errorWrapper.auditErrors))
          )
        )

        result
      }.merge
    }

  private def auditSubmission(details: GenericAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event: AuditEvent[GenericAuditDetail] = AuditEvent(
      auditType = "DeleteLossClaim",
      transactionName = "delete-loss-claim",
      detail = details
    )
    auditService.auditEvent(event)
  }
}
