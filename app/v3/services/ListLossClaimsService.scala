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

package v3.services

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v3.connectors.LossClaimConnector
import v3.models.errors._
import v3.models.requestData.ListLossClaimsRequest

import scala.concurrent.{ExecutionContext, Future}

class ListLossClaimsService @Inject()(connector: LossClaimConnector) extends DownstreamServiceSupport {

  override val serviceName: String = this.getClass.getSimpleName

  def listLossClaims(request: ListLossClaimsRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[ListLossClaimsOutcome] = {
    connector.listLossClaims(request).map {
      mapToVendorDirect("listLossClaims", errorMap)
    }
  }

  private def errorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
    "INVALID_TAXYEAR" -> TaxYearFormatError,
    "INVALID_INCOMESOURCEID" -> BusinessIdFormatError,
    "INVALID_INCOMESOURCETYPE" -> TypeOfLossFormatError,
    "NOT_FOUND" -> NotFoundError,
    "SERVER_ERROR" -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> DownstreamError,
    "INVALID_CLAIM_TYPE" -> ClaimTypeFormatError
  )
}