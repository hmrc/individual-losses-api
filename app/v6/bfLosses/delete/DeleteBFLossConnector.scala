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

package v6.bfLosses.delete

import shared.connectors.DownstreamUri.HipUri
import shared.connectors.httpparsers.StandardDownstreamHttpParser._
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import shared.config.{ConfigFeatureSwitches, SharedAppConfig}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import v6.bfLosses.delete.model.request.DeleteBFLossRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteBFLossConnector @Inject() (val http: HttpClientV2, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def deleteBFLoss(
      request: DeleteBFLossRequestData)(implicit hc: HeaderCarrier, ec: ExecutionContext, correlationId: String): Future[DownstreamOutcome[Unit]] = {

    import request._
    val downstreamUri = if (ConfigFeatureSwitches().isEnabled("hipItsa_hipItsd_migration_1504")) {
      HipUri[Unit](s"itsd/income-sources/brought-forward-losses/$nino/$lossId?taxYear=${taxYear.asTysDownstream}")
    } else {
      HipUri[Unit](s"itsa/income-tax/v1/brought-forward-losses/$nino/${taxYear.asTysDownstream}/$lossId")
    }

    delete(downstreamUri)
  }

}
