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

package api.endpoints.lossClaim.amendOrder.v3

import api.endpoints.lossClaim.amendOrder.v3.request.AmendLossClaimsOrderRequest
import api.endpoints.lossClaim.amendOrder.v3.response.AmendLossClaimsOrderResponse
import api.endpoints.lossClaim.connector.v3.LossClaimConnector
import api.models.ResponseWrapper
import api.models.errors._
import api.models.errors.v3.{RuleInvalidSequenceStart, RuleLossClaimsMissing, RuleSequenceOrderBroken}
import api.services.DownstreamServiceSupport
import api.services.v3.Outcomes.AmendLossClaimsOrderOutcome
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendLossClaimsOrderService @Inject()(connector: LossClaimConnector) extends DownstreamServiceSupport {

  override val serviceName: String = this.getClass.getSimpleName

  def amendLossClaimsOrder(request: AmendLossClaimsOrderRequest)(implicit hc: HeaderCarrier,
                                                                 ec: ExecutionContext): Future[AmendLossClaimsOrderOutcome] = {

    connector
      .amendLossClaimsOrder(request)
      .map {
        mapToVendorDirect("amendLossClaimsOrder", errorMap)
      }
      .map {
        case Left(errorWrapper) => Left(errorWrapper)
        case Right(response)    => Right(ResponseWrapper(response.correlationId, AmendLossClaimsOrderResponse()))
      }
  }

  private def errorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
    "INVALID_TAXYEAR"           -> TaxYearFormatError,
    "CONFLICT_SEQUENCE_START"   -> RuleInvalidSequenceStart,
    "CONFLICT_NOT_SEQUENTIAL"   -> RuleSequenceOrderBroken,
    "CONFLICT_NOT_FULL_LIST"    -> RuleLossClaimsMissing,
    "UNPROCESSABLE_ENTITY"      -> NotFoundError,
    "INVALID_PAYLOAD"           -> StandardDownstreamError,
    "SERVER_ERROR"              -> StandardDownstreamError,
    "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
  )
}