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

package api.endpoints.lossClaim.amendOrder.v3

import api.controllers.{ AuthorisedController, BaseController, EndpointLogContext }
import api.endpoints.lossClaim.amendOrder.v3.request.{ AmendLossClaimsOrderParser, AmendLossClaimsOrderRawData }
import api.endpoints.lossClaim.amendOrder.v3.response.AmendLossClaimsOrderHateoasData
import api.hateoas.HateoasFactory
import api.models.audit.{ AuditEvent, AuditResponse, GenericAuditDetail }
import api.models.errors._
import api.services.{ AuditService, EnrolmentsAuthService, MtdIdLookupService }
import cats.data.EitherT
import cats.implicits._
import play.api.http.MimeTypes
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContentAsJson, ControllerComponents }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.IdGenerator

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class AmendLossClaimsOrderController @Inject()(val authService: EnrolmentsAuthService,
                                               val lookupService: MtdIdLookupService,
                                               amendLossClaimsOrderService: AmendLossClaimsOrderService,
                                               amendLossClaimsOrderParser: AmendLossClaimsOrderParser,
                                               hateoasFactory: HateoasFactory,
                                               auditService: AuditService,
                                               cc: ControllerComponents,
                                               idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "AmendLossClaimsOrderController", endpointName = "Amend a Loss Claim Order")

  def amendClaimsOrder(nino: String, taxYearClaimedFor: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val correlationId: String = idGenerator.getCorrelationId

      val rawData = AmendLossClaimsOrderRawData(nino, taxYearClaimedFor, AnyContentAsJson(request.body))

      val result =
        for {
          parsedRequest   <- EitherT.fromEither[Future](amendLossClaimsOrderParser.parseRequest(rawData))
          serviceResponse <- EitherT(amendLossClaimsOrderService.amendLossClaimsOrder(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory.wrap(serviceResponse.responseData, AmendLossClaimsOrderHateoasData(nino)).asRight[ErrorWrapper])
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          val responseJson: JsValue = Json.toJson(vendorResponse)

          auditSubmission(
            GenericAuditDetail(
              request.userDetails,
              Map("nino" -> nino, "taxYearClaimedFor" -> taxYearClaimedFor),
              Some(request.body),
              serviceResponse.correlationId,
              AuditResponse(httpStatus = OK, response = Right(Some(responseJson)))
            )
          )

          Ok(responseJson)
            .withApiHeaders(serviceResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result        = mapErrorResult(errorWrapper).withApiHeaders(correlationId)

        auditSubmission(
          GenericAuditDetail(
            request.userDetails,
            Map("nino" -> nino, "taxYearClaimedFor" -> taxYearClaimedFor),
            Some(request.body),
            correlationId,
            AuditResponse(httpStatus = result.header.status, response = Left(errorWrapper.auditErrors))
          )
        )

        result
      }.merge
    }

//  private def errorResult(errorWrapper: ErrorWrapper): Result = {
//    errorWrapper.error match {
//      case _
//        if errorWrapper.containsAnyOf(
//          BadRequestError,
//          NinoFormatError,
//          TaxYearFormatError,
//          RuleTaxYearRangeInvalid,
//          RuleTaxYearNotSupportedError,
//          RuleIncorrectOrEmptyBodyError,
//          ClaimIdFormatError,
//          TypeOfClaimFormatError,
//          ValueFormatError,
//          RuleInvalidSequenceStart,
//          RuleSequenceOrderBroken,
//          RuleLossClaimsMissing
//        ) =>
//        BadRequest(Json.toJson(errorWrapper))
//
//      case UnauthorisedError       => Forbidden(Json.toJson(errorWrapper))
//      case NotFoundError           => NotFound(Json.toJson(errorWrapper))
//      case StandardDownstreamError => InternalServerError(Json.toJson(errorWrapper))
//
//      case _ => unhandledError(errorWrapper)
//    }
//  }

  private def auditSubmission(details: GenericAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event: AuditEvent[GenericAuditDetail] = AuditEvent(
      auditType = "AmendLossClaimOrder",
      transactionName = "amend-loss-claim-order",
      detail = details
    )
    auditService.auditEvent(event)
  }
}
