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

package v5.lossClaims.amendType

import shared.connectors.DownstreamUri.{HipUri, IfsUri}
import shared.connectors.httpparsers.StandardDownstreamHttpParser._
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome, DownstreamUri}
import shared.config.{ConfigFeatureSwitches, SharedAppConfig}
import shared.models.domain.TaxYear
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import v5.lossClaims.amendType.model.request.AmendLossClaimTypeRequestData
import v5.lossClaims.amendType.model.response.AmendLossClaimTypeResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendLossClaimTypeConnector @Inject() (val http: HttpClientV2, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def amendLossClaimType(request: AmendLossClaimTypeRequestData, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String
  ): Future[DownstreamOutcome[AmendLossClaimTypeResponse]] = {

    import request._
    import schema._
    val downstreamUri: DownstreamUri[DownstreamResp] = {
      if (ConfigFeatureSwitches().isEnabled("ifs_hip_migration_1506")) {
        HipUri(s"itsd/income-sources/claims-for-relief/$nino/$claimId?taxYear=${taxYear.asTysDownstream}")
      } else {
        IfsUri(s"income-tax/claims-for-relief/$nino/${taxYear.asTysDownstream}/$claimId")
      }
    }

    put(body, downstreamUri)
  }

}
