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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import v1.controllers.requestParsers.ListLossClaimsParser
import v1.models.errors._
import v1.models.requestData.ListLossClaimsRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ListLossClaimsController @Inject()(val authService: EnrolmentsAuthService,
                                         val lookupService: MtdIdLookupService,
                                         listLossClaimsService: ListLossClaimsService,
                                         listLossClaimsParser: ListLossClaimsParser,
                                         auditService: AuditService,
                                         cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  def list(nino: String, taxYear: Option[String], typeOfLoss: Option[String], selfEmploymentId: Option[String]): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      listLossClaimsParser.parseRequest(ListLossClaimsRawData(nino, taxYear = taxYear, typeOfLoss = typeOfLoss, selfEmploymentId = selfEmploymentId)) match {
        case Right(listLossClaimsRequest) =>
          listLossClaimsService.listLossClaims(listLossClaimsRequest).map {
            case Right(desResponse) if desResponse.responseData.claims.isEmpty =>
              logger.info(s"[ListLossClaimsController] Empty response received with correlationId: ${desResponse.correlationId}")
              NotFound(Json.toJson(NotFoundError))
                .withApiHeaders("X-CorrelationId" -> desResponse.correlationId)

            case Right(desResponse) =>
              logger.info(s"[ListLossClaimsController] Success response received with correlationId: ${desResponse.correlationId}")
              Ok(Json.toJson(desResponse.responseData))
                .withApiHeaders("X-CorrelationId" -> desResponse.correlationId)

            case Left(errorWrapper) =>
              val result = processError(errorWrapper).withApiHeaders("X-CorrelationId" -> getCorrelationId(errorWrapper))
              result
          }
        case Left(errorWrapper) =>
          val result = processError(errorWrapper).withApiHeaders("X-CorrelationId" -> getCorrelationId(errorWrapper))
          Future.successful(result)
      }
    }

  private def processError(errorWrapper: ErrorWrapper) = {
    errorWrapper.error match {
      case BadRequestError | NinoFormatError | TaxYearFormatError | TypeOfLossFormatError | SelfEmploymentIdFormatError | RuleSelfEmploymentId |
          RuleTaxYearNotSupportedError | RuleTaxYearRangeExceededError =>
        BadRequest(Json.toJson(errorWrapper))
      case NotFoundError   => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }
}
