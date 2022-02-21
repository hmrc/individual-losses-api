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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import v3.controllers.requestParsers.AmendLossClaimTypeParser
import v3.hateoas.HateoasFactory
import v3.models.audit.{AuditEvent, GenericAuditDetail}
import v3.models.errors._
import v3.models.request.amendLossClaimType.AmendLossClaimTypeRawData
import v3.models.response.amendLossClaimType.AmendLossClaimTypeHateoasData
import v3.services._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendLossClaimTypeController @Inject()(val authService: EnrolmentsAuthService,
                                             val lookupService: MtdIdLookupService,
                                             amendLossClaimTypeService: AmendLossClaimTypeService,
                                             amendLossClaimTypeParser: AmendLossClaimTypeParser,
                                             hateoasFactory: HateoasFactory,
                                             auditService: AuditService,
                                             cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "AmendLossClaimTypeController", endpointName = "Amend a Loss Claim Type")

  def amend(nino: String, claimId: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      val rawData = AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(request.body))
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](amendLossClaimTypeParser.parseRequest(rawData))
          serviceResponse <- EitherT(amendLossClaimTypeService.amendLossClaimType(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory.wrap(serviceResponse.responseData, AmendLossClaimTypeHateoasData(nino, claimId)).asRight[ErrorWrapper])
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          Ok(Json.toJson(vendorResponse))
            .withApiHeaders(serviceResponse.correlationId)
        }

      result.leftMap { errorWrapper =>
        errorResult(errorWrapper).withApiHeaders(getCorrelationId(errorWrapper))
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError
           | NinoFormatError
           | MtdErrorWithCode(RuleIncorrectOrEmptyBodyError.code)
           | ClaimIdFormatError
           | TypeOfClaimFormatError => BadRequest(Json.toJson(errorWrapper))
      case RuleClaimTypeNotChanged | RuleTypeOfClaimInvalid => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: GenericAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("filler", "filler", details)
    auditService.auditEvent(event)
  }
}
