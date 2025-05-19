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

package v4.services

import cats.implicits._
import common.errors.{
  ClaimIdFormatError,
  RuleCSFHLClaimNotSupportedError,
  RuleClaimTypeNotChanged,
  RuleOutsideAmendmentWindow,
  RuleTypeOfClaimInvalid
}
import shared.controllers.RequestContext
import shared.models.domain.TaxYear
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.{BaseService, ServiceOutcome}
import v4.connectors.{AmendLossClaimTypeConnector, RetrieveLossClaimConnector}
import v4.models.request.amendLossClaimType.AmendLossClaimTypeRequestData
import v4.models.request.retrieveLossClaim.RetrieveLossClaimRequestData
import v4.models.response.amendLossClaimType.AmendLossClaimTypeResponse
import v4.models.response.retrieveLossClaim.RetrieveLossClaimResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendLossClaimTypeService @Inject() (retrieveConnector: RetrieveLossClaimConnector, amendConnector: AmendLossClaimTypeConnector)
    extends BaseService {

  def amendLossClaimType(request: AmendLossClaimTypeRequestData)(implicit
      ctx: RequestContext,
      ec: ExecutionContext): Future[ServiceOutcome[AmendLossClaimTypeResponse]] = {
    val retrieveRequest: RetrieveLossClaimRequestData = RetrieveLossClaimRequestData(request.nino, request.claimId)

    for {
      retrieveResult <- retrieveConnector.retrieveLossClaim(request = retrieveRequest, isAmendRequest = true)
      amendResult <- retrieveResult match {
        case Right(ResponseWrapper(_, response: RetrieveLossClaimResponse)) =>
          val taxYear: TaxYear = TaxYear.fromMtd(response.taxYearClaimedFor)
          amendConnector.amendLossClaimType(request, taxYear).map(_.leftMap(mapDownstreamErrors(errorMap ++ hipErrorMap)))

        case Left(error) => Future.successful(Left(mapDownstreamErrors(errorMap)(error)))
      }
    } yield amendResult
  }

  private val errorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
    "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
    "INVALID_PAYLOAD"           -> InternalError,
    "INVALID_CLAIM_TYPE"        -> RuleTypeOfClaimInvalid,
    "NOT_FOUND"                 -> NotFoundError,
    "CONFLICT"                  -> RuleClaimTypeNotChanged,
    "INVALID_CORRELATIONID"     -> InternalError,
    "SERVER_ERROR"              -> InternalError,
    "SERVICE_UNAVAILABLE"       -> InternalError
  )

  private val hipErrorMap: Map[String, MtdError] = Map(
    "1117" -> TaxYearFormatError,
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
