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

package v3.services

import api.controllers.RequestContext
import api.models.domain.TaxYear
import api.models.errors._
import api.services.{BaseService, ServiceOutcome}
import v3.connectors.ListBFLossesConnector
import v3.models.request.listBFLosses.ListBFLossesRequest
import v3.models.response.listBFLosses.{ListBFLossesItem, ListBFLossesResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ListBFLossesService @Inject() (connector: ListBFLossesConnector) extends BaseService {

  def listBFLosses(request: ListBFLossesRequest)(implicit
      ctx: RequestContext,
      ec: ExecutionContext): Future[ServiceOutcome[ListBFLossesResponse[ListBFLossesItem]]] =
    connector
      .listBFLosses(request)
      .map {
        case Left(err) =>
          Left(mapDownstreamErrors(errorMap(request.taxYearBroughtForwardFrom))(err))

        case Right(responseWrapper) if responseWrapper.responseData.losses.isEmpty =>
          Left(ErrorWrapper(ctx.correlationId, NotFoundError))

        case Right(result) =>
          Right(result)
      }

  private def errorMap(maybeTaxYear: Option[TaxYear]): Map[String, MtdError] = {
    val errors = Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "SERVER_ERROR"              -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError,
      "INVALID_TAX_YEAR"          -> TaxYearFormatError,
      "INVALID_INCOMESOURCE_ID"   -> BusinessIdFormatError,
      "INVALID_INCOMESOURCE_TYPE" -> TypeOfLossFormatError,
      "INVALID_CORRELATION_ID"    -> InternalError,
      "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError
    )

    val taxYearErrors = maybeTaxYear match {
      case Some(_) =>
        Map(
          "INVALID_TAX_YEAR"       -> TaxYearFormatError,
          "TAX_YEAR_NOT_SUPPORTED" -> RuleTaxYearNotSupportedError
        )
      case _ =>
        Map(
          "INVALID_TAX_YEAR"       -> InternalError,
          "TAX_YEAR_NOT_SUPPORTED" -> InternalError
        )
    }

    errors ++ taxYearErrors
  }

}
