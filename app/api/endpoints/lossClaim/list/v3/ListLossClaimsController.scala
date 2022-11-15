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

package api.endpoints.lossClaim.list.v3

import api.controllers.{ AuthorisedController, BaseController, EndpointLogContext }
import api.endpoints.lossClaim.list.v3.request.{ ListLossClaimsParser, ListLossClaimsRawData }
import api.endpoints.lossClaim.list.v3.response.ListLossClaimsHateoasData
import api.hateoas.HateoasFactory
import api.models.errors._
import api.services.{ AuditService, EnrolmentsAuthService, MtdIdLookupService }
import cats.data.EitherT
import cats.implicits._
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, ControllerComponents }
import utils.{ IdGenerator, Logging }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class ListLossClaimsController @Inject()(val authService: EnrolmentsAuthService,
                                         val lookupService: MtdIdLookupService,
                                         listLossClaimsService: ListLossClaimsService,
                                         listLossClaimsParser: ListLossClaimsParser,
                                         hateoasFactory: HateoasFactory,
                                         auditService: AuditService,
                                         cc: ControllerComponents,
                                         idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController
    with Logging {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "ListLossClaimsController", endpointName = "List Loss Claims")

  def list(nino: String,
           taxYear: Option[String],
           typeOfLoss: Option[String],
           businessId: Option[String],
           typeOfClaim: Option[String]): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val correlationId: String = idGenerator.getCorrelationId

      val rawData =
        ListLossClaimsRawData(nino, taxYearClaimedFor = taxYear, typeOfLoss = typeOfLoss, businessId = businessId, typeOfClaim = typeOfClaim)

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

      result.leftMap(errorResult).merge
    }
}
