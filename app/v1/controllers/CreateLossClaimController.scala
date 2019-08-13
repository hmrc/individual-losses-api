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

import javax.inject.{Inject, Singleton}
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import v1.controllers.requestParsers.CreateLossClaimParser
import v1.models.errors._
import v1.models.requestData.CreateLossClaimRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateLossClaimController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          createLossClaimService: CreateLossClaimService,
                                          createLossClaimParser: CreateLossClaimParser,
                                          auditService: AuditService,
                                          cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController {


  def create(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      createLossClaimParser.parseRequest(CreateLossClaimRawData(nino, AnyContentAsJson(request.body))) match {
        case Right(createLossClaimRequest) =>
          createLossClaimService.createLossClaim(createLossClaimRequest).map {
            case Right(desResponse) =>
              logger.info(s"[CreateLossClaimController] Success response received with correlationId: ${desResponse.correlationId}")
              Created(Json.toJson(desResponse.responseData))
                .withApiHeaders(desResponse.correlationId)
                .as(MimeTypes.JSON)

            case Left(errorWrapper) =>
              val result = processError(errorWrapper).withApiHeaders(getCorrelationId(errorWrapper))
              result
          }
        case Left(errorWrapper) =>
          val result = processError(errorWrapper).withApiHeaders(getCorrelationId(errorWrapper))
          Future.successful(result)
      }
    }

  private def processError(errorWrapper: ErrorWrapper) = {
    errorWrapper.error match {
      case BadRequestError
           | NinoFormatError
           | TaxYearFormatError
           | RuleIncorrectOrEmptyBodyError
           | RuleTaxYearNotSupportedError
           | RuleTaxYearRangeExceededError
           | TypeOfLossFormatError
           | SelfEmploymentIdFormatError
           | RuleSelfEmploymentId
           | RuleTypeOfClaimInvalid
           | TypeOfClaimFormatError => BadRequest(Json.toJson(errorWrapper))
      case RuleDuplicateClaimSubmissionError | RulePeriodNotEnded | RuleAccountingPeriodNotActive => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }
}
