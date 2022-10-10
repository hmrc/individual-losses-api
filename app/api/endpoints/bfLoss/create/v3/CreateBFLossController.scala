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

package api.endpoints.bfLoss.create.v3

import api.controllers.{AuthorisedController, BaseController, EndpointLogContext}
import api.endpoints.bfLoss.create.v3.request.{CreateBFLossParser, CreateBFLossRawData}
import api.endpoints.bfLoss.create.v3.response.CreateBFLossHateoasData
import api.hateoas.HateoasFactory
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.errors._
import api.models.errors.v3.{RuleDuplicateSubmissionError, ValueFormatError}
import api.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import cats.data.EitherT
import cats.implicits._
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.IdGenerator

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateBFLossController @Inject()(val authService: EnrolmentsAuthService,
                                       val lookupService: MtdIdLookupService,
                                       createBFLossService: CreateBFLossService,
                                       createBFLossParser: CreateBFLossParser,
                                       hateoasFactory: HateoasFactory,
                                       auditService: AuditService,
                                       cc: ControllerComponents,
                                       idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateBFLossController", endpointName = "Create a Brought Forward Loss")

  def create(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val correlationId: String = idGenerator.getCorrelationId
      val rawData = CreateBFLossRawData(nino, AnyContentAsJson(request.body))
      val result =
        for {
          parsedRequest   <- EitherT.fromEither[Future](createBFLossParser.parseRequest(rawData))
          serviceResponse <- EitherT(createBFLossService.createBFLoss(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory
              .wrap(serviceResponse.responseData, CreateBFLossHateoasData(nino, serviceResponse.responseData.lossId))
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
      case BadRequestError | NinoFormatError | TaxYearFormatError | MtdErrorWithCode(RuleIncorrectOrEmptyBodyError.code) | MtdErrorWithCode(
            RuleTaxYearNotSupportedError.code) | RuleTaxYearRangeInvalid | TypeOfLossFormatError | BusinessIdFormatError | RuleBusinessId |
          MtdErrorWithCode(ValueFormatError.code) | RuleTaxYearNotEndedError | MtdErrorWithCode(TaxYearFormatError.code) | MtdErrorWithCode(
            RuleTaxYearRangeInvalid.code) =>
        BadRequest(Json.toJson(errorWrapper))
      case RuleDuplicateSubmissionError => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError                => NotFound(Json.toJson(errorWrapper))
      case StandardDownstreamError      => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: GenericAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event: AuditEvent[GenericAuditDetail] = AuditEvent(
      auditType = "CreateBroughtForwardLoss",
      transactionName = "create-brought-forward-loss",
      detail = details
    )
    auditService.auditEvent(event)
  }
}
