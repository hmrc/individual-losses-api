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

package api.endpoints.lossClaim.connector.v3

import api.connectors.DownstreamUri.{IfsUri, TaxYearSpecificIfsUri}
import api.connectors.httpparsers.StandardDownstreamHttpParser._
import api.connectors.{BaseDownstreamConnector, DownstreamOutcome, DownstreamUri}
import api.endpoints.lossClaim.list.v3.request.ListLossClaimsRequest
import api.endpoints.lossClaim.list.v3.response.{ListLossClaimsItem, ListLossClaimsResponse}
import config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ListLossClaimsConnector @Inject()(val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def listLossClaims(request: ListLossClaimsRequest)(implicit
                                                     hc: HeaderCarrier,
                                                     ec: ExecutionContext,
                                                     correlationId: String): Future[DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]]] = {
    import request._

    val pathParameters = Map(
      "taxYear"          -> taxYearClaimedFor.map(_.asDownstream),
      "incomeSourceId"   -> businessId,
      "incomeSourceType" -> typeOfLoss.flatMap(_.toIncomeSourceType).map(_.toString),
      "claimType"        -> typeOfClaim.map(_.toReliefClaimed.toString)
    ).collect {
      case (key, Some(value)) => key -> value
    }

    val (uri: DownstreamUri[ListLossClaimsResponse[ListLossClaimsItem]], downstreamSpecificParameters) = taxYearClaimedFor match {
      case Some(taxYear) if taxYear.useTaxYearSpecificApi =>
        (
          TaxYearSpecificIfsUri[ListLossClaimsResponse[ListLossClaimsItem]](s"income-tax/claims-for-relief/${taxYear.asTysDownstream}/${nino.nino}"),
          pathParameters.filterNot { case (key, _) => key == "taxYear" }
        )
      case _ => (IfsUri[ListLossClaimsResponse[ListLossClaimsItem]](s"income-tax/claims-for-relief/${nino.nino}"), pathParameters)
    }

    get(uri, downstreamSpecificParameters.toSeq)
  }

}
