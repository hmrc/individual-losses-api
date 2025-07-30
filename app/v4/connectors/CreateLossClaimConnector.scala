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

import shared.config.{ConfigFeatureSwitches, SharedAppConfig}
import shared.connectors.DownstreamUri.{HipUri, IfsUri}
import shared.connectors.httpparsers.StandardDownstreamHttpParser.*
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import shared.models.domain.TaxYear
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import v4.models.request.createLossClaim.CreateLossClaimRequestData
import v4.models.response.createLossClaim.CreateLossClaimResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateLossClaimConnector @Inject() (val http: HttpClientV2, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def createLossClaim(request: CreateLossClaimRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[CreateLossClaimResponse]] = {
    import request.*

    val taxYear: TaxYear = TaxYear.fromMtd(request.lossClaim.taxYearClaimedFor)

    val downstreamUri = if (ConfigFeatureSwitches().isEnabled("ifs_hip_migration_1505")) {
      post(lossClaim, HipUri[CreateLossClaimResponse](s"itsd/income-sources/claims-for-relief/$nino?taxYear=${taxYear.asTysDownstream}"))
    } else {
      post(lossClaim, IfsUri[CreateLossClaimResponse](s"income-tax/claims-for-relief/$nino"))
    }

    downstreamUri
  }

}
