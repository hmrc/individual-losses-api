/*
 * Copyright 2023 HM Revenue & Customs
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

package v4.services

import common.errors.{RuleInvalidSequenceStart, RuleLossClaimsMissing, RuleOutsideAmendmentWindow, RuleSequenceOrderBroken}
import shared.controllers.RequestContext
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.{BaseService, ServiceOutcome}
import v4.connectors.AmendLossClaimsConnector
import v4.models.request.amendLossClaimsOrder.AmendLossClaimsOrderRequestData
import v4.models.response.amendLossClaimsOrder.AmendLossClaimsOrderResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendLossClaimsOrderService @Inject() (connector: AmendLossClaimsConnector) extends BaseService {

  def amendLossClaimsOrder(request: AmendLossClaimsOrderRequestData)(implicit
      ctx: RequestContext,
      ec: ExecutionContext): Future[ServiceOutcome[AmendLossClaimsOrderResponse]] = {

    connector
      .amendLossClaimsOrder(request)
      .map {
        case Left(err)       => Left(mapDownstreamErrors(errorMap)(err))
        case Right(response) => Right(ResponseWrapper(response.correlationId, AmendLossClaimsOrderResponse()))
      }
  }

  private val errorMap: Map[String, MtdError] = Map(
    "1215" -> NinoFormatError,
    "1117" -> TaxYearFormatError,
    "1216" -> InternalError,
    "1000" -> InternalError,
    "1108" -> NotFoundError,
    "1109" -> RuleSequenceOrderBroken,
    "1110" -> RuleInvalidSequenceStart,
    "1111" -> RuleLossClaimsMissing,
    "4200" -> RuleOutsideAmendmentWindow,
    "5000" -> RuleTaxYearNotSupportedError
  )

}
