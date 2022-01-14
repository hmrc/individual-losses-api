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

import cats.data.EitherT
import cats.implicits._
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import v3.controllers.requestParsers.AmendLossClaimsOrderParser
import v3.hateoas.HateoasFactory
import v3.models.audit.{AmendLossClaimsOrderAuditDetail, AuditEvent, AuditResponse}
import v3.models.downstream.AmendLossClaimsOrderHateoasData
import v3.models.errors._
import v3.models.requestData.AmendLossClaimsOrderRawData
import v3.services.{AmendLossClaimsOrderService, AuditService, EnrolmentsAuthService, MtdIdLookupService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendLossClaimsOrderController @Inject()(val authService: EnrolmentsAuthService,
                                         val lookupService: MtdIdLookupService,
                                         amendLossClaimsOrderService: AmendLossClaimsOrderService,
                                         amendLossClaimsOrderParser: AmendLossClaimsOrderParser,
                                         hateoasFactory: HateoasFactory,
                                         auditService: AuditService,
                                         cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "AmendLossClaimsOrderController", endpointName = "Amend a Loss Claim Order")

  def amendClaimsOrder(nino: String, taxYearClaimedFor: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      val rawData = AmendLossClaimsOrderRawData(nino, taxYearClaimedFor, AnyContentAsJson(request.body))
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](amendLossClaimsOrderParser.parseRequest(rawData))
          serviceResponse <- EitherT(amendLossClaimsOrderService.amendLossClaimsOrder(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory.wrap(serviceResponse.responseData, AmendLossClaimsOrderHateoasData(nino)).asRight[ErrorWrapper])
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          val response = Json.toJson(vendorResponse)

          auditSubmission(AmendLossClaimsOrderAuditDetail(request.userDetails, nino, taxYearClaimedFor, request.body,
            serviceResponse.correlationId, AuditResponse(OK, Right(Some(response)))))

          Ok(response)
            .withApiHeaders(serviceResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result = errorResult(errorWrapper).withApiHeaders(correlationId)
        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError
           | NinoFormatError
           | TaxYearFormatError
           | MtdErrorWithCode(RuleIncorrectOrEmptyBodyError.code)
           | MtdErrorWithCode(ClaimIdFormatError.code)
           | TypeOfClaimFormatError
           | MtdErrorWithCode(ValueFormatError.code)
           | RuleInvalidSequenceStart
           | RuleSequenceOrderBroken
           | RuleLossClaimsMissing => BadRequest(Json.toJson(errorWrapper))
      case UnauthorisedError => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: AmendLossClaimsOrderAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext) = {
    val event = AuditEvent("AmendLossClaimsOrder", "amend-loss-claims-order", details)
    auditService.auditEvent(event)
  }
}