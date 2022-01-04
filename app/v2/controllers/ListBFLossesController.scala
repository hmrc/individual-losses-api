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

package v2.controllers

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.http.HeaderCarrier
import v2.controllers.requestParsers.ListBFLossesParser
import v2.hateoas.HateoasFactory
import v2.models.audit.{AuditEvent, AuditResponse, ListBFLossesAuditDetail}
import v2.models.des.ListBFLossHateoasData
import v2.models.errors._
import v2.models.requestData.ListBFLossesRawData
import v2.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ListBFLossesController @Inject()(val authService: EnrolmentsAuthService,
                                       val lookupService: MtdIdLookupService,
                                       listBFLossesService: ListBFLossesService,
                                       listBFLossesParser: ListBFLossesParser,
                                       hateoasFactory: HateoasFactory,
                                       auditService: AuditService,
                                       cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "LisBFLossesController", endpointName = "List Brought Forward Losses")

  def list(nino: String, taxYear: Option[String], typeOfLoss: Option[String], businessId: Option[String]): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      val rawData = ListBFLossesRawData(nino, taxYear = taxYear, typeOfLoss = typeOfLoss, businessId = businessId)
      val result =
        for {
          parsedRequest   <- EitherT.fromEither[Future](listBFLossesParser.parseRequest(rawData))
          serviceResponse <- EitherT(listBFLossesService.listBFLosses(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory
              .wrapList(serviceResponse.responseData, ListBFLossHateoasData(nino))
              .asRight[ErrorWrapper]
          )
        } yield {
          if (vendorResponse.payload.losses.isEmpty) {
            logger.info(
              s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
                s"Empty response received with correlationId: ${serviceResponse.correlationId}")

            NotFound(Json.toJson(NotFoundError))
              .withApiHeaders(serviceResponse.correlationId)
          } else {
            logger.info(
              s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
                s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

            val response = Json.toJson(vendorResponse)

            auditSubmission(ListBFLossesAuditDetail(request.userDetails, nino, taxYear, typeOfLoss, businessId,
              serviceResponse.correlationId, AuditResponse(OK, Right(Some(response)))))

            Ok(response)
              .withApiHeaders(serviceResponse.correlationId)
              .as(MimeTypes.JSON)
          }
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result        = errorResult(errorWrapper).withApiHeaders(correlationId)
        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError | NinoFormatError | TaxYearFormatError | TypeOfLossFormatError | BusinessIdFormatError |
           RuleTaxYearNotSupportedError | RuleTaxYearRangeInvalid =>
        BadRequest(Json.toJson(errorWrapper))
      case NotFoundError   => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: ListBFLossesAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext) = {
    val event = AuditEvent("ListBroughtForwardLosses", "list-brought-forward-Losses", details)
    auditService.auditEvent(event)
  }
}
