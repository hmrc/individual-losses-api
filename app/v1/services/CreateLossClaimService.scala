/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.services

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v1.connectors.LossClaimConnector
import v1.models.errors._
import v1.models.requestData.CreateLossClaimRequest

import scala.concurrent.{ExecutionContext, Future}

class CreateLossClaimService @Inject()(connector: LossClaimConnector) extends DesServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def createLossClaim(request: CreateLossClaimRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CreateLossClaimOutcome] = {
    connector.createLossClaim(request).map {
      mapToVendorDirect("createLossClaim", mappingDesToMtdError)
    }
  }

  private def mappingDesToMtdError: PartialFunction[String, MtdError] =
     {
       case "INVALID_TAXABLE_ENTITY_ID"   => NinoFormatError
       case "DUPLICATE"                   => RuleDuplicateClaimSubmissionError
       case "NOT_FOUND_INCOME_SOURCE"     => NotFoundError
       case "ACCOUNTING_PERIOD_NOT_ENDED" => RulePeriodNotEnded
       case "INVALID_CLAIM_TYPE"          => RuleTypeOfClaimInvalid
       case "TAX_YEAR_NOT_SUPPORTED"      => RuleTaxYearNotSupportedError
       case "NO_ACCOUNTING_PERIOD"        => RuleNoAccountingPeriod
       case "INVALID_PAYLOAD" | "SERVER_ERROR"  | "SERVICE_UNAVAILABLE" => DownstreamError
      // Likely to be removed as they do not exist in the latest swagger 01/08/2019 or are pertaining to brought forward losses
       case error@("INVALID_TAX_YEAR" | "INCOME_SOURCE_NOT_ACTIVE" | "TAX_YEAR_NOT_ENDED") =>
         logger.info(s"[$serviceName] [Unexpected error: $error]")
         DownstreamError
    }
}
