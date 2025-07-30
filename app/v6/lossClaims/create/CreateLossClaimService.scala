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

package v6.lossClaims.create

import cats.implicits.*
import common.errors.*
import shared.controllers.RequestContext
import shared.models.errors.*
import shared.services.{BaseService, ServiceOutcome}
import v6.lossClaims.create.model.request.CreateLossClaimRequestData
import v6.lossClaims.create.model.response.CreateLossClaimResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateLossClaimService @Inject() (connector: CreateLossClaimConnector) extends BaseService {

  def createLossClaim(
      request: CreateLossClaimRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[CreateLossClaimResponse]] =
    connector
      .createLossClaim(request)
      .map(_.leftMap(mapDownstreamErrors(errorMap)))

  private val errorMap: PartialFunction[String, MtdError] = {
    case "INVALID_TAXABLE_ENTITY_ID"   => NinoFormatError
    case "INVALID_TAX_YEAR"            => TaxYearClaimedForFormatError
    case "DUPLICATE"                   => RuleDuplicateClaimSubmissionError
    case "INCOME_SOURCE_NOT_FOUND"     => NotFoundError
    case "ACCOUNTING_PERIOD_NOT_ENDED" => RulePeriodNotEnded
    case "INVALID_CLAIM_TYPE"          => RuleTypeOfClaimInvalid
    case "TAX_YEAR_NOT_SUPPORTED"      => RuleTaxYearNotSupportedError
    case "NO_ACCOUNTING_PERIOD"        => RuleNoAccountingPeriod
    case "INVALID_PAYLOAD"             => InternalError
    case "SERVER_ERROR"                => InternalError
    case "SERVICE_UNAVAILABLE"         => InternalError
    case "INVALID_CORRELATIONID"       => InternalError
    case "CSFHL_CLAIM_NOT_SUPPORTED"   => RuleCSFHLClaimNotSupportedError
    case "OUTSIDE_AMENDMENT_WINDOW"    => RuleOutsideAmendmentWindow

    case "1215" => NinoFormatError
    case "1002" => NotFoundError
    case "1117" => TaxYearClaimedForFormatError
    case "1127" => RuleCSFHLClaimNotSupportedError
    case "1228" => RuleDuplicateClaimSubmissionError
    case "1104" => RulePeriodNotEnded
    case "1105" => RuleTypeOfClaimInvalid
    case "1106" => RuleNoAccountingPeriod
    case "1107" => RuleTaxYearNotSupportedError
    case "4200" => RuleOutsideAmendmentWindow
    case "5000" => RuleTaxYearNotSupportedError
  }

}
