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

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import v1.controllers.requestParsers.CreateBFLossParser
import v1.models.errors._
import v1.models.requestData.CreateBFLossRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateBFLossController @Inject()(val authService: EnrolmentsAuthService,
                                       val lookupService: MtdIdLookupService,
                                       createBFLossService: CreateBFLossService,
                                       createBFLossParser: CreateBFLossParser,
                                       auditService: AuditService,
                                       cc: ControllerComponents)(implicit ec: ExecutionContext) extends AuthorisedController(cc) {

  protected val logger: Logger = Logger(this.getClass)

  def create(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      createBFLossParser.parseRequest(CreateBFLossRawData(nino, AnyContentAsJson(request.body))) match {
        case Right(createBFLossRequest) => createBFLossService.createBFLoss(createBFLossRequest).map {
          case Right(desResponse) =>
            logger.info(s"[CreateBFLossController] Success response received with correlationId: ${desResponse.correlationId}")
            Created(Json.toJson(desResponse.responseData))
              .withHeaders("X-CorrelationId" -> desResponse.correlationId).as(MimeTypes.JSON)

          case Left(errorWrapper) =>
            val result = processError(errorWrapper).withHeaders("X-CorrelationId" -> getCorrelationId(errorWrapper))
            result
        }
        case Left(errorWrapper) =>
          val result = processError(errorWrapper).withHeaders("X-CorrelationId" -> getCorrelationId(errorWrapper))
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
           | TypeOfLossUnsupportedFormatError
           | SelfEmploymentIdFormatError
           | RuleSelfEmploymentId
           | AmountFormatError
           | RuleInvalidLossAmount => BadRequest(Json.toJson(errorWrapper))
      case RuleDuplicateSubmissionError => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def getCorrelationId(errorWrapper: ErrorWrapper): String = {
    errorWrapper.correlationId match {
      case Some(correlationId) => logger.info("[CreateBFLossController][getCorrelationId] - " +
        s"Error received from DES ${Json.toJson(errorWrapper)} with correlationId: $correlationId")
        correlationId
      case None =>
        val correlationId = UUID.randomUUID().toString
        logger.info("[CreateBFLossController][getCorrelationId] - " +
          s"Validation error: ${Json.toJson(errorWrapper)} with correlationId: $correlationId")
        correlationId
    }
  }
}
