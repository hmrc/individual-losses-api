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

package v5.lossClaims.amendOrder

import shared.connectors.DownstreamUri.{HipUri, TaxYearSpecificIfsUri}
import shared.connectors.httpparsers.StandardDownstreamHttpParser._
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome, DownstreamUri}
import shared.config.{ConfigFeatureSwitches, SharedAppConfig}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import v5.lossClaims.amendOrder.model.request.AmendLossClaimsOrderRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendLossClaimsOrderConnector @Inject() (val http: HttpClientV2, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def amendLossClaimsOrder(request: AmendLossClaimsOrderRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[Unit]] = {

    import request._

    val downstreamUri: DownstreamUri[Unit] = if (ConfigFeatureSwitches().isEnabled("ifs_hip_migration_1793")) {
      HipUri[Unit](s"itsd/income-sources/claims-for-relief/$nino/preferences?taxYear=${taxYearClaimedFor.asTysDownstream}")
    } else {
      TaxYearSpecificIfsUri[Unit](s"income-tax/claims-for-relief/preferences/${taxYearClaimedFor.asTysDownstream}/$nino")
    }

    put(body, downstreamUri)
  }

}
