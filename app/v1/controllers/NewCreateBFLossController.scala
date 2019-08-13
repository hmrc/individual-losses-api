/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.Logging
import v1.controllers.requestParsers.CreateBFLossParser
import v1.models.audit.{AuditEvent, AuditResponse, CreateBFLossAuditDetail}
import v1.models.auth.UserDetails
import v1.models.errors._
import v1.models.requestData.CreateBFLossRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NewCreateBFLossController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          createBFLossService: CreateBFLossService,
                                          createBFLossParser: CreateBFLossParser,
                                          auditService: AuditService,
                                          cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController with Logging {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "SampleController", endpointName = "sampleEndpoint")

  def create(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      val rawData = CreateBFLossRawData(nino, AnyContentAsJson(request.body))
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](createBFLossParser.parseRequest(rawData))
          vendorResponse <- EitherT(createBFLossService.createBFLoss(parsedRequest))
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${vendorResponse.correlationId}")
          auditSubmission(createAuditDetails(rawData, CREATED, vendorResponse.correlationId, request.userDetails))

          Created(Json.toJson(vendorResponse.responseData))
            .withApiHeaders(vendorResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result = errorResult(errorWrapper).withApiHeaders(correlationId)
        auditSubmission(createAuditDetails(rawData, result.header.status, correlationId, request.userDetails, Some(errorWrapper)))
        result
      }.merge
    }


  private def errorResult(errorWrapper: ErrorWrapper) = {
    errorWrapper.error match {
      case BadRequestError | NinoFormatError | TaxYearFormatError | RuleIncorrectOrEmptyBodyError | RuleTaxYearNotSupportedError |
           RuleTaxYearRangeExceededError | TypeOfLossFormatError | SelfEmploymentIdFormatError | RuleSelfEmploymentId | AmountFormatError |
           RuleInvalidLossAmount | RuleTaxYearNotEndedError =>
        BadRequest(Json.toJson(errorWrapper))
      case RuleDuplicateSubmissionError => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def createAuditDetails(rawData: CreateBFLossRawData,
                                 statusCode: Int,
                                 correlationId: String,
                                 userDetails: UserDetails,
                                 errorWrapper: Option[ErrorWrapper] = None): CreateBFLossAuditDetail = {
    val response = errorWrapper
      .map { wrapper =>
        AuditResponse(statusCode, Some(wrapper.auditErrors))
      }
      .getOrElse(AuditResponse(statusCode, None))

    CreateBFLossAuditDetail(userDetails.userType, userDetails.agentReferenceNumber, rawData.nino, correlationId, response)
  }

  private def auditSubmission(details: CreateBFLossAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("sampleAuditType", "sample-transaction-type", details)
    auditService.auditEvent(event)
  }

}
