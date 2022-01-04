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
import v3.models.requestData.DeleteBFLossRequest

import scala.concurrent.{ExecutionContext, Future}

class DeleteBFLossService @Inject()(connector: BFLossConnector) extends DownstreamServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def deleteBFLoss(request: DeleteBFLossRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DeleteBFLossOutcome] = {

    connector.deleteBFLoss(request).map {
      mapToVendorDirect("deleteBFLoss", errorMap)
    }
  }

  private def errorMap: Map[String, MtdError] = Map(
    "INVALID_IDVALUE"     -> NinoFormatError,
    "INVALID_LOSS_ID"     -> LossIdFormatError,
    "NOT_FOUND"           -> NotFoundError,
    "CONFLICT"            -> RuleDeleteAfterCrystallisationError,
    "SERVER_ERROR"        -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> DownstreamError
  )
}
