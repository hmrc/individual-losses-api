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
import v1.connectors.BFLossConnector
import v1.models.errors._
import v1.models.requestData.CreateBFLossRequest

import scala.concurrent.{ExecutionContext, Future}

class CreateBFLossService @Inject()(connector: BFLossConnector) extends DesServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def createBFLoss(request: CreateBFLossRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CreateBFLossOutcome] = {

    connector.createBFLoss(request).map {
      mapToVendorDirect("createBFLoss", mappingDesToMtdError)
    }
  }

  private def mappingDesToMtdError: PartialFunction[String, MtdError] = {
    case "INVALID_TAXABLE_ENTITY_ID" => NinoFormatError
    case "DUPLICATE" => RuleDuplicateSubmissionError
    case "TAX_YEAR_NOT_SUPPORTED" => RuleTaxYearNotSupportedError
    case "NOT_FOUND_INCOME_SOURCE" => NotFoundError
    case "TAX_YEAR_NOT_ENDED" => RuleTaxYearNotEndedError
    case "INVALID_PAYLOAD" | "SERVER_ERROR" | "SERVICE_UNAVAILABLE" => DownstreamError
    case error@("INVALID_TAX_YEAR" | "INCOME_SOURCE_NOT_ACTIVE") => // Likely to be removed as they do not exist in the latest swagger 01/08/2019
      logger.info(s"[$serviceName] [Unexpected error: $error]")
      DownstreamError
    case error@("NO_ACCOUNTING_PERIOD" | "INVALID_CLAIM_TYPE" | "ACCOUNTING_PERIOD_NOT_ENDED") =>
      logger.info(s"[$serviceName] [Unexpected error: $error]")
      DownstreamError
  }
}
