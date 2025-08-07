/*
 * Copyright 2023 HM Revenue & Customs
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

package v4.services

import common.errors.TypeOfLossFormatError
import shared.controllers.RequestContext
import shared.models.errors.*
import shared.services.{BaseService, ServiceOutcome}
import v4.connectors.ListBFLossesConnector
import v4.models.request.listLossClaims.ListBFLossesRequestData
import v4.models.response.listBFLosses.{ListBFLossesItem, ListBFLossesResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ListBFLossesService @Inject() (connector: ListBFLossesConnector) extends BaseService {

  def listBFLosses(request: ListBFLossesRequestData)(implicit
      ctx: RequestContext,
      ec: ExecutionContext): Future[ServiceOutcome[ListBFLossesResponse[ListBFLossesItem]]] =
    connector
      .listBFLosses(request)
      .map {
        case Left(err) =>
          Left(mapDownstreamErrors(errorMap)(err))

        case Right(responseWrapper) if responseWrapper.responseData.losses.isEmpty =>
          Left(ErrorWrapper(ctx.correlationId, NotFoundError))

        case Right(result) =>
          Right(result)
      }

  private val errorMap: Map[String, MtdError] =
    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAX_YEAR"          -> TaxYearFormatError,
      "INVALID_INCOMESOURCE_ID"   -> BusinessIdFormatError,
      "INVALID_INCOMESOURCE_TYPE" -> TypeOfLossFormatError,
      "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError,
      "NOT_FOUND"                 -> NotFoundError,
      "INVALID_CORRELATION_ID"    -> InternalError,
      "SERVER_ERROR"              -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError
    )

}
