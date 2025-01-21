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

package v6.lossClaims.list

import shared.connectors.DownstreamUri.TaxYearSpecificIfsUri
import shared.connectors.httpparsers.StandardDownstreamHttpParser._
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome, DownstreamUri}
import shared.config.SharedAppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v6.lossClaims.list.model.request.ListLossClaimsRequestData
import v6.lossClaims.list.model.response.ListLossClaimsResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ListLossClaimsConnector @Inject() (val http: HttpClient, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def listLossClaims(request: ListLossClaimsRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[ListLossClaimsResponse]] = {

    import request._
    import schema._

    val downstreamUri: DownstreamUri[DownstreamResp] = TaxYearSpecificIfsUri(
      s"income-tax/${taxYearClaimedFor.asTysDownstream}/claims-for-relief/${nino.nino}")

    val params = List(
      "incomeSourceId"   -> businessId.map(_.businessId),
      "incomeSourceType" -> typeOfLoss.flatMap(_.toIncomeSourceType).map(_.toString),
      "claimType"        -> typeOfClaim.map(_.toReliefClaimed.toString)
    ).collect { case (key, Some(value)) =>
      key -> value
    }
    get(
      downstreamUri,
      queryParams = params
    )
  }

}
