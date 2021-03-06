/*
 * Copyright 2021 HM Revenue & Customs
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

package v2.services

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.LossClaimConnector
import v2.models.errors._
import v2.models.requestData.DeleteLossClaimRequest

import scala.concurrent.{ExecutionContext, Future}

class DeleteLossClaimService @Inject()(connector: LossClaimConnector) extends DesServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def deleteLossClaim(request: DeleteLossClaimRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DeleteLossClaimOutcome] = {

    connector.deleteLossClaim(request).map {
      mapToVendorDirect("deleteLossClaim", mappingDesToMtdError)
    }
  }

  private def mappingDesToMtdError: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID"  -> NinoFormatError,
    "INVALID_CLAIM_ID"           -> ClaimIdFormatError,
    "NOT_FOUND"                  -> NotFoundError,
    "SERVER_ERROR"               -> DownstreamError,
    "SERVICE_UNAVAILABLE"        -> DownstreamError
  )
}
