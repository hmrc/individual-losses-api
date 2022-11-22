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

package api.endpoints.lossClaim.create.v3

import api.endpoints.lossClaim.connector.v3.LossClaimConnector
import api.endpoints.lossClaim.create.v3.request.CreateLossClaimRequest
import api.models.errors._
import api.services.DownstreamServiceSupport
import api.services.v3.Outcomes.CreateLossClaimOutcome
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class CreateLossClaimService @Inject()(connector: LossClaimConnector) extends DownstreamServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def createLossClaim(
      request: CreateLossClaimRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext, correlationId: String): Future[CreateLossClaimOutcome] = {
    connector.createLossClaim(request).map {
      mapToVendorDirect("createLossClaim", errorMap)
    }
  }

  private def errorMap: PartialFunction[String, MtdError] = {
    case "INVALID_TAXABLE_ENTITY_ID"                                                          => NinoFormatError
    case "DUPLICATE"                                                                          => RuleDuplicateClaimSubmissionError
    case "INCOME_SOURCE_NOT_FOUND"                                                            => NotFoundError
    case "ACCOUNTING_PERIOD_NOT_ENDED"                                                        => RulePeriodNotEnded
    case "INVALID_CLAIM_TYPE"                                                                 => RuleTypeOfClaimInvalid
    case "TAX_YEAR_NOT_SUPPORTED"                                                             => RuleTaxYearNotSupportedError
    case "NO_ACCOUNTING_PERIOD"                                                               => RuleNoAccountingPeriod
    case "INVALID_PAYLOAD" | "SERVER_ERROR" | "SERVICE_UNAVAILABLE" | "INVALID_CORRELATIONID" => InternalError
  }
}
