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

package v6.lossClaims.amendType

import cats.implicits.*
import common.errors._
import shared.controllers.RequestContext
import shared.models.errors.*
import shared.services.{BaseService, ServiceOutcome}
import v6.lossClaims.amendType.model.request.AmendLossClaimTypeRequestData
import v6.lossClaims.amendType.model.response.AmendLossClaimTypeResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendLossClaimTypeService @Inject() (connector: AmendLossClaimTypeConnector) extends BaseService {

  def amendLossClaimType(request: AmendLossClaimTypeRequestData)(implicit
      ctx: RequestContext,
      ec: ExecutionContext): Future[ServiceOutcome[AmendLossClaimTypeResponse]] =
    connector
      .amendLossClaimType(request)
      .map(_.leftMap(mapDownstreamErrors(errorMap ++ hipErrorMap)))

  private val errorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
    "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
    "INVALID_PAYLOAD"           -> InternalError,
    "INVALID_CLAIM_TYPE"        -> RuleTypeOfClaimInvalid,
    "INVALID_TAX_YEAR"          -> TaxYearClaimedForFormatError,
    "CSFHL_CLAIM_NOT_SUPPORTED" -> RuleCSFHLClaimNotSupportedError,
    "NOT_FOUND"                 -> NotFoundError,
    "CONFLICT"                  -> RuleClaimTypeNotChanged,
    "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
    "INVALID_CORRELATIONID"     -> InternalError,
    "SERVER_ERROR"              -> InternalError,
    "SERVICE_UNAVAILABLE"       -> InternalError
  )

  private val hipErrorMap: Map[String, MtdError] = Map(
    "1117" -> TaxYearClaimedForFormatError,
    "1215" -> NinoFormatError,
    "1216" -> InternalError,
    "1220" -> ClaimIdFormatError,
    "5010" -> NotFoundError,
    "1000" -> InternalError,
    "1105" -> RuleTypeOfClaimInvalid,
    "1127" -> RuleCSFHLClaimNotSupportedError,
    "1228" -> RuleClaimTypeNotChanged,
    "4200" -> RuleOutsideAmendmentWindow,
    "5000" -> RuleTaxYearNotSupportedError
  )

}
