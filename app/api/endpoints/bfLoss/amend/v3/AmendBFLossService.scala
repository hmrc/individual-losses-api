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

package api.endpoints.bfLoss.amend.v3

import api.endpoints.bfLoss.amend.v3.request.AmendBFLossRequest
import api.endpoints.bfLoss.connector.v3.BFLossConnector
import api.models.errors._
import api.models.errors.v3.RuleLossAmountNotChanged
import api.services.DownstreamServiceSupport
import api.services.v3.Outcomes.AmendBFLossOutcome
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendBFLossService @Inject()(connector: BFLossConnector) extends DownstreamServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def amendBFLoss(request: AmendBFLossRequest)(implicit
                                               hc: HeaderCarrier,
                                               ec: ExecutionContext,
                                               correlationId: String): Future[AmendBFLossOutcome] = {

    connector.amendBFLoss(request).map {
      mapToVendorDirect("amendBFLoss", errorMap)
    }
  }

  private def errorMap: Map[String, MtdError] =
    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_LOSS_ID"           -> LossIdFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "CONFLICT"                  -> RuleLossAmountNotChanged,
      "INVALID_CORRELATIONID"     -> StandardDownstreamError,
      "INVALID_PAYLOAD"           -> StandardDownstreamError,
      "SERVER_ERROR"              -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
    )
}
