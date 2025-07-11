/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.list

import common.errors.{TaxYearClaimedForFormatError, TypeOfClaimFormatError, TypeOfLossFormatError}
import shared.controllers.RequestContext
import shared.models.errors._
import shared.services.{BaseService, ServiceOutcome}
import v6.lossClaims.list.def1.response.Def1_ListLossClaimsResponse
import v6.lossClaims.list.model.request.ListLossClaimsRequestData
import v6.lossClaims.list.model.response.ListLossClaimsResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ListLossClaimsService @Inject() (connector: ListLossClaimsConnector) extends BaseService {

  def listLossClaims(
      request: ListLossClaimsRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[ListLossClaimsResponse]] =
    connector
      .listLossClaims(request)
      .map {
        case Left(err) =>
          Left(mapDownstreamErrors(errorMap)(err))

        case Right(responseWrapper) if responseWrapper.responseData == Def1_ListLossClaimsResponse(List()) =>
          Left(ErrorWrapper(ctx.correlationId, NotFoundError))

        case Right(result) =>
          Right(result)
      }

  private val errorMap: Map[String, MtdError] =
    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_CLAIM_TYPE"        -> TypeOfClaimFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "SERVER_ERROR"              -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError,
      "INVALID_CORRELATION_ID"    -> InternalError,
      "INVALID_INCOMESOURCE_ID"   -> BusinessIdFormatError,
      "INVALID_INCOMESOURCE_TYPE" -> TypeOfLossFormatError,
      "INVALID_TAX_YEAR"          -> TaxYearClaimedForFormatError,
      "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError
    )

}
