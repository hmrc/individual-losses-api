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

package v6.bfLosses.amend

import shared.connectors.DownstreamUri.{HipUri, IfsUri}
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome, DownstreamUri}
import shared.config.{ConfigFeatureSwitches, SharedAppConfig}
import uk.gov.hmrc.http.HeaderCarrier
import v6.bfLosses.amend.model.request.AmendBFLossRequestData
import v6.bfLosses.amend.model.response.AmendBFLossResponse
import shared.connectors.httpparsers.StandardDownstreamHttpParser.reads
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendBFLossConnector @Inject() (val http: HttpClientV2, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def amendBFLoss(request: AmendBFLossRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[AmendBFLossResponse]] = {

    import request.*
    import schema.*

    val downstreamUri: DownstreamUri[DownstreamResp] =
      if (ConfigFeatureSwitches().isEnabled("ifs_hip_migration_1501")) {
        HipUri(s"itsd/income-sources/brought-forward-losses/$nino/$lossId?taxYear=${taxYear.asTysDownstream}")
      } else {
        IfsUri(s"income-tax/brought-forward-losses/$nino/${taxYear.asTysDownstream}/$lossId")
      }

    put(amendBroughtForwardLoss, downstreamUri)
  }

}
