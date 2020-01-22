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
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import v1.controllers.requestParsers.CreateLossClaimParser
import v1.hateoas.HateoasFactory
import v1.models.audit.{AuditEvent, AuditResponse, CreateLossClaimAuditDetail}
import v1.models.des.CreateLossClaimHateoasData
import v1.models.errors._
import v1.models.requestData.CreateLossClaimRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateLossClaimController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          createLossClaimService: CreateLossClaimService,
                                          createLossClaimParser: CreateLossClaimParser,
                                          hateoasFactory: HateoasFactory,
                                          auditService: AuditService,
                                          cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateLossClaimController", endpointName = "Create a Loss Claim")

  def create(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      val rawData = CreateLossClaimRawData(nino, AnyContentAsJson(request.body))
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](createLossClaimParser.parseRequest(rawData))
          serviceResponse <- EitherT(createLossClaimService.createLossClaim(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory
              .wrap(serviceResponse.responseData, CreateLossClaimHateoasData(nino, serviceResponse.responseData.id))
              .asRight[ErrorWrapper]
          )
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          val response = Json.toJson(vendorResponse)

          auditSubmission(CreateLossClaimAuditDetail(request.userDetails, nino, request.body,
            serviceResponse.correlationId, AuditResponse(CREATED, Right(Some(response)))))

          Created(response)
            .withApiHeaders(serviceResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result = errorResult(errorWrapper).withApiHeaders(correlationId)

        auditSubmission(CreateLossClaimAuditDetail(request.userDetails, nino, request.body,
          correlationId, AuditResponse(result.header.status, Left(errorWrapper.auditErrors))))

        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError
           | NinoFormatError
           | TaxYearFormatError
           | RuleIncorrectOrEmptyBodyError
           | RuleTaxYearNotSupportedError
           | RuleTaxYearRangeInvalid
           | TypeOfLossFormatError
           | SelfEmploymentIdFormatError
           | RuleSelfEmploymentId
           | RuleTypeOfClaimInvalid
           | TypeOfClaimFormatError =>
        BadRequest(Json.toJson(errorWrapper))
      case RuleDuplicateClaimSubmissionError | RulePeriodNotEnded | RuleNoAccountingPeriod => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: CreateLossClaimAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext) = {
    val event = AuditEvent("createLossClaim", "create-loss-claim", details)
    auditService.auditEvent(event)
  }
}
