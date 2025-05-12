/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.amendOrder

import _root_.common.errors._
import cats.implicits._
import shared.controllers.RequestContext
import shared.models.errors._
import shared.services.{BaseService, ServiceOutcome}
import v6.lossClaims.amendOrder.model.request.AmendLossClaimsOrderRequestData

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendLossClaimsOrderService @Inject() (connector: AmendLossClaimsOrderConnector) extends BaseService {

  def amendLossClaimsOrder(
      request: AmendLossClaimsOrderRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {

    connector
      .amendLossClaimsOrder(request)
      .map(_.leftMap(mapDownstreamErrors(errorMap)))

  }

  private val errorMap: Map[String, MtdError] = {
    val ifsErrors = Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAX_YEAR"          -> TaxYearFormatError,
      "INVALID_PAYLOAD"           -> InternalError,
      "INVALID_CORRELATIONID"     -> InternalError,
      "NOT_FOUND"                 -> NotFoundError,
      "NOT_SEQUENTIAL"            -> RuleSequenceOrderBroken,
      "SEQUENCE_START"            -> RuleInvalidSequenceStart,
      "NO_FULL_LIST"              -> RuleLossClaimsMissing,
      "CLAIM_NOT_FOUND"           -> NotFoundError,
      "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError,
      "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
      "SERVER_ERROR"              -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError
    )

    val hipErrors = Map(
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

    ifsErrors ++ hipErrors
  }

}
