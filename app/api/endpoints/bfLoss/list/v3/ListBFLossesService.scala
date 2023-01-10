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

package api.endpoints.bfLoss.list.v3

import api.controllers.RequestContext
import api.endpoints.bfLoss.connector.v3.BFLossConnector
import api.endpoints.bfLoss.list.v3.request.ListBFLossesRequest
import api.models.errors._
import api.services.BaseService
import api.services.v3.Outcomes.ListBFLossesOutcome

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class ListBFLossesService @Inject() (connector: BFLossConnector) extends BaseService {

  def listBFLosses(request: ListBFLossesRequest)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ListBFLossesOutcome] =
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

  private val errorMap: Map[String, MtdError] = {
    val errors = Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAXYEAR"           -> TaxYearFormatError,
      "INVALID_INCOMESOURCEID"    -> BusinessIdFormatError,
      "INVALID_INCOMESOURCETYPE"  -> TypeOfLossFormatError,
      "INVALID_CORRELATIONID"     -> InternalError,
      "NOT_FOUND"                 -> NotFoundError,
      "SERVER_ERROR"              -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError
    )

    val extraTysErrors = Map(
      "INVALID_TAX_YEAR"          -> TaxYearFormatError,
      "INVALID_INCOMESOURCE_ID"   -> BusinessIdFormatError,
      "INVALID_INCOMESOURCE_TYPE" -> TypeOfLossFormatError,
      "INVALID_CORRELATION_ID"    -> InternalError,
      "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError
    )

    errors ++ extraTysErrors
  }

}
