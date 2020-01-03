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
import v1.models.requestData.AmendBFLossRequest

import scala.concurrent.{ExecutionContext, Future}

class AmendBFLossService @Inject()(connector: BFLossConnector) extends DesServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def amendBFLoss(request: AmendBFLossRequest)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[AmendBFLossOutcome] = {

    connector.amendBFLoss(request).map {
      mapToVendorDirect("amendBFLoss", mappingDesToMtdError)
    }
  }

  private def mappingDesToMtdError: Map[String, MtdError] =
  Map(
    "INVALID_TAXABLE_ENTITY_ID"  -> NinoFormatError,
    "INVALID_LOSS_ID"            -> LossIdFormatError,
    "NOT_FOUND"                  -> NotFoundError,
    "CONFLICT"                   -> RuleLossAmountNotChanged,
    "INVALID_PAYLOAD"            -> DownstreamError,
    "SERVER_ERROR"               -> DownstreamError,
    "SERVICE_UNAVAILABLE"        -> DownstreamError
  )
}
