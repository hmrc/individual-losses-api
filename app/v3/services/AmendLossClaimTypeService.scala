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

import api.connectors.v3.LossClaimConnector
import api.models.errors._
import api.services.DownstreamServiceSupport
import uk.gov.hmrc.http.HeaderCarrier
import v3.models.errors._
import v3.models.request.amendLossClaimType.AmendLossClaimTypeRequest

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendLossClaimTypeService @Inject()(connector: LossClaimConnector) extends DownstreamServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def amendLossClaimType(request: AmendLossClaimTypeRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AmendLossClaimTypeOutcome] = {

    connector.amendLossClaimType(request).map {
      mapToVendorDirect("amendLossClaimType", errorMap)
    }
  }

  private def errorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
    "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
    "INVALID_PAYLOAD"           -> StandardDownstreamError,
    "INVALID_CLAIM_TYPE"        -> RuleTypeOfClaimInvalid,
    "NOT_FOUND"                 -> NotFoundError,
    "CONFLICT"                  -> RuleClaimTypeNotChanged,
    "INVALID_CORRELATIONID"     -> StandardDownstreamError,
    "SERVER_ERROR"              -> StandardDownstreamError,
    "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
  )
}
