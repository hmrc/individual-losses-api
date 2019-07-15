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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import v1.controllers.requestParsers.RetrieveLossClaimParser
import v1.models.errors._
import v1.models.requestData.RetrieveLossClaimRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveLossClaimController @Inject()(val authService: EnrolmentsAuthService,
                                            val lookupService: MtdIdLookupService,
                                            retrieveLossClaimService: RetrieveLossClaimService,
                                            retrieveLossClaimParser: RetrieveLossClaimParser,
                                            auditService: AuditService,
                                            cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  protected val logger: Logger = Logger(this.getClass)

  def retrieve(nino: String, lossId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      retrieveLossClaimParser.parseRequest(RetrieveLossClaimRawData(nino, lossId)) match {
        case Right(retrieveLossClaimRequest) =>
          retrieveLossClaimService.retrieveLossClaim(retrieveLossClaimRequest).map {
            case Right(desResponse) =>
              logger.info(s"[RetrieveLossClaimController] Success response received with correlationId: ${desResponse.correlationId}")
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
      case BadRequestError | NinoFormatError | ClaimIdFormatError => BadRequest(Json.toJson(errorWrapper))
      case NotFoundError                                          => NotFound(Json.toJson(errorWrapper))
      case DownstreamError                                        => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def getCorrelationId(errorWrapper: ErrorWrapper): String = {
    errorWrapper.correlationId match {
      case Some(correlationId) =>
        logger.info(
          "[RetrieveLossClaimController][getCorrelationId] - " +
            s"Error received from DES ${Json.toJson(errorWrapper)} with correlationId: $correlationId")
        correlationId
      case None =>
        val correlationId = UUID.randomUUID().toString
        logger.info(
          "[RetrieveLossClaimController][getCorrelationId] - " +
            s"Validation error: ${Json.toJson(errorWrapper)} with correlationId: $correlationId")
        correlationId
    }
  }
}
