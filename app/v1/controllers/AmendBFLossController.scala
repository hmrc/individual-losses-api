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
import v1.controllers.requestParsers.AmendBFLossParser
import v1.hateoas.HateoasFactory
import v1.models.audit.{AmendBFLossAuditDetail, AuditEvent, AuditResponse}
import v1.models.des.AmendBFLossHateoasData
import v1.models.errors._
import v1.models.requestData.AmendBFLossRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendBFLossController @Inject()(val authService: EnrolmentsAuthService,
                                      val lookupService: MtdIdLookupService,
                                      amendBFLossService: AmendBFLossService,
                                      amendBFLossParser: AmendBFLossParser,
                                      hateoasFactory: HateoasFactory,
                                      auditService: AuditService,
                                      cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "AmendBFLossController", endpointName = "Amend a Brought Forward Loss Amount")

  def amend(nino: String, lossId: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      val rawData = AmendBFLossRawData(nino, lossId, AnyContentAsJson(request.body))
      val result =
        for {
          parsedRequest   <- EitherT.fromEither[Future](amendBFLossParser.parseRequest(rawData))
          serviceResponse <- EitherT(amendBFLossService.amendBFLoss(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory.wrap(serviceResponse.responseData, AmendBFLossHateoasData(nino, lossId)).asRight[ErrorWrapper])
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          val response = Json.toJson(vendorResponse)

          auditSubmission(AmendBFLossAuditDetail(request.userDetails, nino, lossId, request.body,
            serviceResponse.correlationId, AuditResponse(OK, Right(Some(response)))))

          Ok(response)
            .withApiHeaders(serviceResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result        = errorResult(errorWrapper).withApiHeaders(correlationId)

        auditSubmission(AmendBFLossAuditDetail(request.userDetails, nino, lossId, request.body,
          correlationId, AuditResponse(result.header.status, Left(errorWrapper.auditErrors))))

        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError | NinoFormatError | RuleIncorrectOrEmptyBodyError | LossIdFormatError | AmountFormatError | RuleInvalidLossAmount =>
        BadRequest(Json.toJson(errorWrapper))
      case RuleLossAmountNotChanged => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError            => NotFound(Json.toJson(errorWrapper))
      case DownstreamError          => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: AmendBFLossAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext) = {
    val event = AuditEvent("amendBroughtForwardLoss", "amend-brought-forward-Loss", details)
    auditService.auditEvent(event)
  }
}
