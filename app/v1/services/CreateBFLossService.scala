/*
 * Copyright 2019 HM Revenue & Customs
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

  private def mappingDesToMtdError: Map[String, MtdError] =
    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "DUPLICATE"                 -> RuleDuplicateSubmissionError,
      "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError,
      "NOT_FOUND_INCOME_SOURCE"   -> NotFoundError,
      "TAX_YEAR_NOT_ENDED"        -> RuleTaxYearNotEndedError,
      "INVALID_PAYLOAD"           -> DownstreamError,
      "SERVER_ERROR"              -> DownstreamError,
      "SERVICE_UNAVAILABLE"       -> DownstreamError,
      "INVALID_TAX_YEAR"          -> DownstreamError,
      "INCOME_SOURCE_NOT_ACTIVE"  -> DownstreamError,
      "ACCOUNTING_PERIOD_NOT_ENDED" -> DownstreamError,
      "INVALID_CLAIM_TYPE"        -> DownstreamError,
      "NO_ACTIVE_ACCOUNTING_PERIOD" -> DownstreamError
    )
}
