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
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, ControllerComponents }
import v1.controllers.requestParsers.ListLossClaimsParser
import v1.hateoas.HateoasFactory
import v1.models.des.ListLossClaimsHateoasData
import v1.models.errors._
import v1.models.requestData.ListLossClaimsRawData
import v1.services._

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class ListLossClaimsController @Inject()(val authService: EnrolmentsAuthService,
                                         val lookupService: MtdIdLookupService,
                                         listLossClaimsService: ListLossClaimsService,
                                         listLossClaimsParser: ListLossClaimsParser,
                                         hateoasFactory: HateoasFactory,
                                         auditService: AuditService,
                                         cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "ListLossClaimsController", endpointName = "List Loss Claims")

  def list(nino: String, taxYear: Option[String], typeOfLoss: Option[String], selfEmploymentId: Option[String]): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      val rawData = ListLossClaimsRawData(nino, taxYear = taxYear, typeOfLoss = typeOfLoss, selfEmploymentId = selfEmploymentId)
      val result =
        for {
          parsedRequest   <- EitherT.fromEither[Future](listLossClaimsParser.parseRequest(rawData))
          serviceResponse <- EitherT(listLossClaimsService.listLossClaims(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory
              .wrapList(serviceResponse.responseData, ListLossClaimsHateoasData(nino))
              .asRight[ErrorWrapper]
          )
        } yield {
          if (vendorResponse.payload.claims.isEmpty) {
            logger.info(
              s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
                s"Empty response received with correlationId: ${serviceResponse.correlationId}")

            NotFound(Json.toJson(NotFoundError))
              .withApiHeaders(serviceResponse.correlationId)
          } else {
            logger.info(
              s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
                s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

            Ok(Json.toJson(vendorResponse))
              .withApiHeaders(serviceResponse.correlationId)
          }
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result        = errorResult(errorWrapper).withApiHeaders(correlationId)
        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    errorWrapper.error match {
      case BadRequestError | NinoFormatError | TaxYearFormatError | TypeOfLossFormatError | SelfEmploymentIdFormatError | RuleSelfEmploymentId |
          RuleTaxYearNotSupportedError | RuleTaxYearRangeExceededError =>
        BadRequest(Json.toJson(errorWrapper))
      case NotFoundError   => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }
}
