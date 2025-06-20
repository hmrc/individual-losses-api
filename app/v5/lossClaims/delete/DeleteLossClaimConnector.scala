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

package v5.lossClaims.delete

import shared.config.{ConfigFeatureSwitches, SharedAppConfig}
import shared.connectors.DownstreamUri.HipUri
import shared.connectors.httpparsers.StandardDownstreamHttpParser._
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import shared.models.domain.TaxYear
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import v5.lossClaims.delete.model.request.DeleteLossClaimRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteLossClaimConnector @Inject() (val http: HttpClientV2, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def deleteLossClaim(request: DeleteLossClaimRequestData, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String
  ): Future[DownstreamOutcome[Unit]] = {

    import request._

    val downstreamUri = if (ConfigFeatureSwitches().isEnabled("hipItsa_hipItsd_migration_1509")) {
      HipUri[Unit](s"itsd/income-sources/claims-for-relief/$nino/$claimId?taxYear=${taxYear.asTysDownstream}")
    } else {
      HipUri[Unit](s"itsa/income-tax/v1/claims-for-relief/$nino/${taxYear.asTysDownstream}/$claimId")
    }

    delete(downstreamUri)
  }

}
