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

package api.endpoints.lossClaim.retrieve.v3

import api.controllers.{ AuthorisedController, BaseController, EndpointLogContext }
import api.endpoints.lossClaim.retrieve.v3.request.{ RetrieveLossClaimParser, RetrieveLossClaimRawData }
import api.endpoints.lossClaim.retrieve.v3.response.GetLossClaimHateoasData
import api.hateoas.HateoasFactory
import api.models.errors._
import api.services.{ AuditService, EnrolmentsAuthService, MtdIdLookupService }
import cats.data.EitherT
import cats.implicits._
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, ControllerComponents }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class RetrieveLossClaimController @Inject()(val authService: EnrolmentsAuthService,
                                            val lookupService: MtdIdLookupService,
                                            retrieveLossClaimService: RetrieveLossClaimService,
                                            retrieveLossClaimParser: RetrieveLossClaimParser,
                                            hateoasFactory: HateoasFactory,
                                            auditService: AuditService,
                                            cc: ControllerComponents)(implicit
                                                                      ec: ExecutionContext,
                                                                      correlationId: String)
    extends AuthorisedController(cc)
    with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "RetrieveLossClaimController", endpointName = "Retrieve a Loss Claim")

  def retrieve(nino: String, claimId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      val rawData = RetrieveLossClaimRawData(nino, claimId)
      val result =
        for {
          parsedRequest   <- EitherT.fromEither[Future](retrieveLossClaimParser.parseRequest(rawData))
          serviceResponse <- EitherT(retrieveLossClaimService.retrieveLossClaim(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory.wrap(serviceResponse.responseData, GetLossClaimHateoasData(nino, claimId)).asRight[ErrorWrapper])
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          Ok(Json.toJson(vendorResponse))
            .withApiHeaders(serviceResponse.correlationId)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result        = errorResult(errorWrapper).withApiHeaders(correlationId)
        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    errorWrapper.error match {
      case BadRequestError | NinoFormatError | ClaimIdFormatError => BadRequest(Json.toJson(errorWrapper))
      case NotFoundError                                          => NotFound(Json.toJson(errorWrapper))
      case StandardDownstreamError                                => InternalServerError(Json.toJson(errorWrapper))
      case _                                                      => unhandledError(errorWrapper)
    }
  }
}
