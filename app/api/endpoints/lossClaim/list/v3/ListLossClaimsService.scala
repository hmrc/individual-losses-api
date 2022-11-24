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

package api.endpoints.lossClaim.list.v3

import api.endpoints.lossClaim.connector.v3.LossClaimConnector
import api.endpoints.lossClaim.list.v3.request.ListLossClaimsRequest
import api.models.errors._
import api.services.DownstreamServiceSupport
import api.services.v3.Outcomes.ListLossClaimsOutcome
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class ListLossClaimsService @Inject()(connector: LossClaimConnector) extends DownstreamServiceSupport {

  override val serviceName: String = this.getClass.getSimpleName

  def listLossClaims(request: ListLossClaimsRequest)(implicit
                                                     hc: HeaderCarrier,
                                                     ec: ExecutionContext,
                                                     correlationId: String): Future[ListLossClaimsOutcome] = {
    connector.listLossClaims(request).map {
      mapToVendorDirect("listLossClaims", errorMap)
    }
  }

  private def errorMap: Map[String, MtdError] = {
    val downstreamErrors = Map(
      "INVALID_TAXABLE_ENTITY_ID"   -> NinoFormatError,
      "INVALID_TAXYEAR"             -> TaxYearFormatError,
      "INVALID_INCOMESOURCEID"      -> BusinessIdFormatError,
      "INVALID_INCOMESOURCETYPE"    -> TypeOfLossFormatError,
      "INVALID_CLAIM_TYPE"          -> TypeOfClaimFormatError,
      "NOT_FOUND"                   -> NotFoundError,
      "INVALID_CORRELATIONID"       -> InternalError,
      "SERVER_ERROR"                -> InternalError,
      "SERVICE_UNAVAILABLE"         -> InternalError,
      "RULE_TAX_YEAR_RANGE_INVALID" -> RuleTaxYearRangeInvalidError
    )

    val tysErrors = Map(
      "INVALID_CORRELATION_ID"    -> InternalError,
      "INVALID_TAX_YEAR"          -> TaxYearFormatError,
      "INVALID_INCOMESOURCE_ID"   -> BusinessIdFormatError,
      "INVALID_INCOMESOURCE_TYPE" -> TypeOfLossFormatError,
      "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError
    )

    downstreamErrors ++ tysErrors
  }
}
