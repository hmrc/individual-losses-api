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

package v5.lossClaims.retrieve

import shared.connectors.DownstreamUri.{HipUri, IfsUri}
import shared.connectors.httpparsers.StandardDownstreamHttpParser.reads
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome, DownstreamUri}
import shared.config.{ConfigFeatureSwitches, SharedAppConfig}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import v5.lossClaims.retrieve.model.request.RetrieveLossClaimRequestData
import v5.lossClaims.retrieve.model.response.RetrieveLossClaimResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveLossClaimConnector @Inject() (val http: HttpClientV2, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def retrieveLossClaim(request: RetrieveLossClaimRequestData, isAmendRequest: Boolean = false)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[RetrieveLossClaimResponse]] = {

    import request.*
    import schema.*

    lazy val maybeIntent: Option[String] =
      if (isAmendRequest && ConfigFeatureSwitches().isEnabled("passIntentHeader")) Some("AMEND_LOSS_CLAIM") else None

    lazy val downstreamUri: DownstreamUri[DownstreamResp] =
      if (ConfigFeatureSwitches().isEnabled("ifs_hip_migration_1508")) {
        HipUri(s"itsd/income-sources/claims-for-relief/$nino/$claimId")
      } else {
        IfsUri(s"income-tax/claims-for-relief/$nino/$claimId")
      }

    get(uri = downstreamUri, maybeIntent = maybeIntent)
  }

}
