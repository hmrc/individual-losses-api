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

package v5.lossClaims.list

import api.connectors.DownstreamUri.TaxYearSpecificIfsUri
import api.connectors.httpparsers.StandardDownstreamHttpParser._
import api.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v4.models.request.listLossClaims.ListLossClaimsRequestData
import v4.models.response.listLossClaims.{ListLossClaimsItem, ListLossClaimsResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ListLossClaimsConnector @Inject() (val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def listLossClaims(request: ListLossClaimsRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]]] = {

    import request._

    val params = List(
      "incomeSourceId"   -> businessId.map(_.businessId),
      "incomeSourceType" -> typeOfLoss.flatMap(_.toIncomeSourceType).map(_.toString),
      "claimType"        -> typeOfClaim.map(_.toReliefClaimed.toString)
    ).collect { case (key, Some(value)) =>
      key -> value
    }

    get(
      uri = TaxYearSpecificIfsUri(s"income-tax/${taxYearClaimedFor.asTysDownstream}/claims-for-relief/${nino.nino}"),
      queryParams = params
    )
  }

}
