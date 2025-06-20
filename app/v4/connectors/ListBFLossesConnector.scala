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

package v4.connectors

import shared.config.SharedAppConfig
import shared.connectors.DownstreamUri.TaxYearSpecificIfsUri
import shared.connectors.httpparsers.StandardDownstreamHttpParser._
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import v4.models.request.listLossClaims.ListBFLossesRequestData
import v4.models.response.listBFLosses.{ListBFLossesItem, ListBFLossesResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ListBFLossesConnector @Inject() (val http: HttpClientV2, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def listBFLosses(request: ListBFLossesRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]]] = {

    import request._

    val queryParams = List(
      businessId.map("incomeSourceId"         -> _.businessId),
      incomeSourceType.map("incomeSourceType" -> _.toString)
    ).flatten

    get(
      TaxYearSpecificIfsUri[ListBFLossesResponse[ListBFLossesItem]](
        s"income-tax/brought-forward-losses/${taxYearBroughtForwardFrom.asTysDownstream}/$nino"
      ),
      queryParams
    )

  }

}
