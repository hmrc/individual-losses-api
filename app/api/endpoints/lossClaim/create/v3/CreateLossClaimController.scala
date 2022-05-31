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

package api.endpoints.lossClaim.create.v3

import api.controllers.{ AuthorisedController, BaseController, EndpointLogContext }
import api.endpoints.lossClaim.create.v3.request.{ CreateLossClaimParser, CreateLossClaimRawData }
import api.endpoints.lossClaim.create.v3.response.CreateLossClaimHateoasData
import api.hateoas.HateoasFactory
import api.models.audit.{ AuditEvent, AuditResponse, GenericAuditDetail }
import api.models.errors._
import api.models.errors.v3._
import api.services.{ AuditService, EnrolmentsAuthService, MtdIdLookupService }
import cats.data.EitherT
import cats.implicits._
import play.api.http.MimeTypes
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContentAsJson, ControllerComponents }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class CreateLossClaimController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          createLossClaimService: CreateLossClaimService,
                                          createLossClaimParser: CreateLossClaimParser,
                                          hateoasFactory: HateoasFactory,
                                          auditService: AuditService,
                                          cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateLossClaimController", endpointName = "Create a Loss Claim")

  def create(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      val rawData = CreateLossClaimRawData(nino, AnyContentAsJson(request.body))
      val result =
        for {
          parsedRequest   <- EitherT.fromEither[Future](createLossClaimParser.parseRequest(rawData))
          serviceResponse <- EitherT(createLossClaimService.createLossClaim(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory
              .wrap(serviceResponse.responseData, CreateLossClaimHateoasData(nino, serviceResponse.responseData.claimId))
              .asRight[ErrorWrapper]
          )
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          val responseJson: JsValue = Json.toJson(vendorResponse)

          auditSubmission(
            GenericAuditDetail(
              request.userDetails,
              Map("nino" -> nino),
              Some(request.body),
              serviceResponse.correlationId,
              AuditResponse(httpStatus = CREATED, response = Right(Some(responseJson)))
            )
          )

          Created(responseJson)
            .withApiHeaders(serviceResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result        = errorResult(errorWrapper).withApiHeaders(correlationId)

        auditSubmission(
          GenericAuditDetail(
            request.userDetails,
            Map("nino" -> nino),
            Some(request.body),
            correlationId,
            AuditResponse(httpStatus = result.header.status, response = Left(errorWrapper.auditErrors))
          )
        )

        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    errorWrapper.error match {
      case BadRequestError | NinoFormatError | TaxYearClaimedForFormatError | MtdErrorWithCode(RuleIncorrectOrEmptyBodyError.code) |
          RuleTaxYearNotSupportedError | RuleTaxYearRangeInvalid | TypeOfLossFormatError | BusinessIdFormatError | RuleTypeOfClaimInvalid |
          TypeOfClaimFormatError | MtdErrorWithCode(TaxYearClaimedForFormatError.code) | MtdErrorWithCode(RuleTaxYearRangeInvalid.code) =>
        BadRequest(Json.toJson(errorWrapper))
      case RuleDuplicateClaimSubmissionError | RulePeriodNotEnded | RuleNoAccountingPeriod => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError                                                                   => NotFound(Json.toJson(errorWrapper))
      case StandardDownstreamError                                                         => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: GenericAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event: AuditEvent[GenericAuditDetail] = AuditEvent(
      auditType = "CreateLossClaim",
      transactionName = "create-loss-claim",
      detail = details
    )
    auditService.auditEvent(event)
  }
}