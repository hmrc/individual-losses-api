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
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import v1.controllers.requestParsers.AmendLossClaimParser
import v1.models.errors._
import v1.models.requestData.AmendLossClaimRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendLossClaimController @Inject()(val authService: EnrolmentsAuthService,
                                         val lookupService: MtdIdLookupService,
                                         amendLossClaimService: AmendLossClaimService,
                                         amendLossClaimParser: AmendLossClaimParser,
                                         auditService: AuditService,
                                         cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController {

  def amend(nino: String, lossId: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      amendLossClaimParser.parseRequest(AmendLossClaimRawData(nino, lossId, AnyContentAsJson(request.body))) match {
        case Right(amendLossClaimRequest) => amendLossClaimService.amendLossClaim(amendLossClaimRequest).map {
          case Right(desResponse) =>
            logger.info(s"[AmendLossClaimController] Success response received with correlationId: ${desResponse.correlationId}")
            Ok(Json.toJson(desResponse.responseData))
              .withApiHeaders(desResponse.correlationId).as(MimeTypes.JSON)

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
           | RuleIncorrectOrEmptyBodyError
           | ClaimIdFormatError
           | TypeOfClaimFormatError => BadRequest(Json.toJson(errorWrapper))
      case RuleClaimTypeNotChanged | RuleTypeOfClaimInvalid   => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }
}
