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
import v3.controllers.requestParsers.CreateBFLossParser
import v3.hateoas.HateoasFactory
import v3.models.downstream.CreateBFLossHateoasData
import v3.models.errors._
import v3.models.requestData.CreateBFLossRawData
import v3.services._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateBFLossController @Inject()(val authService: EnrolmentsAuthService,
                                       val lookupService: MtdIdLookupService,
                                       createBFLossService: CreateBFLossService,
                                       createBFLossParser: CreateBFLossParser,
                                       hateoasFactory: HateoasFactory,
                                       cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateBFLossController", endpointName = "Create a Brought Forward Loss")

  def create(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
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

          Created(Json.toJson(vendorResponse))
            .withApiHeaders(serviceResponse.correlationId)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        errorResult(errorWrapper).withApiHeaders(correlationId)
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError | NinoFormatError | TaxYearFormatError | MtdErrorWithCode(RuleIncorrectOrEmptyBodyError.code) | MtdErrorWithCode(
            RuleTaxYearNotSupportedError.code) | RuleTaxYearRangeInvalid | TypeOfLossFormatError | BusinessIdFormatError | RuleBusinessId |
          MtdErrorWithCode(ValueFormatError.code) | RuleTaxYearNotEndedError | MtdErrorWithCode(TaxYearFormatError.code) | MtdErrorWithCode(
            RuleTaxYearRangeInvalid.code) =>
        BadRequest(Json.toJson(errorWrapper))
      case RuleDuplicateSubmissionError => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError                => NotFound(Json.toJson(errorWrapper))
      case DownstreamError              => InternalServerError(Json.toJson(errorWrapper))
    }
  }
}
