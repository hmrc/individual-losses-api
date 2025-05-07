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

package v5.bfLosses.amend

import shared.connectors.DownstreamUri.IfsUri
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome, DownstreamUri}
import shared.config.SharedAppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v5.bfLosses.amend.model.request.AmendBFLossRequestData
import v5.bfLosses.amend.model.response.AmendBFLossResponse
import shared.connectors.httpparsers.StandardDownstreamHttpParser.reads
import shared.models.domain.TaxYear.currentTaxYear

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendBFLossConnector @Inject() (val http: HttpClient, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def amendBFLoss(request: AmendBFLossRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[AmendBFLossResponse]] = {

    import request._
    import schema._
    val downstreamUri: DownstreamUri[DownstreamResp] = IfsUri(s"income-tax/brought-forward-losses/$nino/${currentTaxYear.asTysDownstream}/$lossId")
    put(amendBroughtForwardLoss, downstreamUri)

  }

}
