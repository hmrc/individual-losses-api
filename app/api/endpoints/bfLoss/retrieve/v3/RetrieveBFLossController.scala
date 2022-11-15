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

package api.endpoints.bfLoss.retrieve.v3

import api.controllers.{AuthorisedController, BaseController, EndpointLogContext}
import api.endpoints.bfLoss.retrieve.v3.request.{RetrieveBFLossParser, RetrieveBFLossRawData}
import api.endpoints.bfLoss.retrieve.v3.response.GetBFLossHateoasData
import api.hateoas.HateoasFactory
import api.models.errors._
import api.services.{EnrolmentsAuthService, MtdIdLookupService}
import cats.data.EitherT
import cats.implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.IdGenerator

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveBFLossController @Inject()(val authService: EnrolmentsAuthService,
                                         val lookupService: MtdIdLookupService,
                                         retrieveBFLossService: RetrieveBFLossService,
                                         retrieveBFLossParser: RetrieveBFLossParser,
                                         hateoasFactory: HateoasFactory,
                                         cc: ControllerComponents,
                                         idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "RetrieveBFLossController", endpointName = "Retrieve a Brought Forward Loss")

  def retrieve(nino: String, lossId: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val correlationId: String = idGenerator.getCorrelationId

      val rawData = RetrieveBFLossRawData(nino, lossId)

      val result =
        for {
          parsedRequest   <- EitherT.fromEither[Future](retrieveBFLossParser.parseRequest(rawData))
          serviceResponse <- EitherT(retrieveBFLossService.retrieveBFLoss(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory.wrap(serviceResponse.responseData, GetBFLossHateoasData(nino, lossId)).asRight[ErrorWrapper])
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
}
