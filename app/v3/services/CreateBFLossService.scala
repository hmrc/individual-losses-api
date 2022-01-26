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

package v3.services

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v3.connectors.BFLossConnector
import v3.models.errors._
import v3.models.request.createBFLoss.CreateBFLossRequest

import scala.concurrent.{ExecutionContext, Future}

class CreateBFLossService @Inject()(connector: BFLossConnector) extends DownstreamServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def createBFLoss(request: CreateBFLossRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CreateBFLossOutcome] = {

    connector.createBFLoss(request).map {
      mapToVendorDirect("createBFLoss", errorMap)
    }
  }

  private def errorMap: PartialFunction[String, MtdError] = {
    case "INVALID_TAXABLE_ENTITY_ID" => NinoFormatError
    case "DUPLICATE_SUBMISSION" => RuleDuplicateSubmissionError
    case "TAX_YEAR_NOT_SUPPORTED" => RuleTaxYearNotSupportedError
    case "INCOME_SOURCE_NOT_FOUND" => NotFoundError
    case "TAX_YEAR_NOT_ENDED" => RuleTaxYearNotEndedError
    case "INVALID_CORRELATIONID" => DownstreamError
    case "INVALID_PAYLOAD" => DownstreamError
    case "SERVER_ERROR" => DownstreamError
    case "SERVICE_UNAVAILABLE" => DownstreamError
  }
}
