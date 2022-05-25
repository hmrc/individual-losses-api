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

package api.endpoints.bfLoss.create.v3

import api.endpoints.bfLoss.connector.v3.BFLossConnector
import api.endpoints.bfLoss.create.v3.request.CreateBFLossRequest
import api.models.errors._
import api.models.errors.v3.RuleDuplicateSubmissionError
import api.services.DownstreamServiceSupport
import api.services.v3.Outcomes.CreateBFLossOutcome
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
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
    case "DUPLICATE_SUBMISSION"      => RuleDuplicateSubmissionError
    case "TAX_YEAR_NOT_SUPPORTED"    => RuleTaxYearNotSupportedError
    case "INCOME_SOURCE_NOT_FOUND"   => NotFoundError
    case "TAX_YEAR_NOT_ENDED"        => RuleTaxYearNotEndedError
    case "INVALID_CORRELATIONID"     => StandardDownstreamError
    case "INVALID_PAYLOAD"           => StandardDownstreamError
    case "SERVER_ERROR"              => StandardDownstreamError
    case "SERVICE_UNAVAILABLE"       => StandardDownstreamError
  }
}
