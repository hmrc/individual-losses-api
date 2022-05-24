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

package api.endpoints.bfLoss.list.v3

import api.endpoints.bfLoss.connector.v3.BFLossConnector
import api.endpoints.bfLoss.list.v3.request.ListBFLossesRequest
import api.models.errors._
import api.services.DownstreamServiceSupport
import uk.gov.hmrc.http.HeaderCarrier
import api.services.v3.Outcomes.ListBFLossesOutcome

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class ListBFLossesService @Inject()(connector: BFLossConnector) extends DownstreamServiceSupport {

  override val serviceName: String = this.getClass.getSimpleName

  def listBFLosses(request: ListBFLossesRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ListBFLossesOutcome] = {
    connector.listBFLosses(request).map {
      mapToVendorDirect("listBFLosses", errorMap)
    }
  }

  private def errorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
    "INVALID_TAXYEAR"           -> TaxYearFormatError,
    "INVALID_INCOMESOURCEID"    -> BusinessIdFormatError,
    "INVALID_INCOMESOURCETYPE"  -> TypeOfLossFormatError,
    "NOT_FOUND"                 -> NotFoundError,
    "SERVER_ERROR"              -> StandardDownstreamError,
    "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
  )
}
