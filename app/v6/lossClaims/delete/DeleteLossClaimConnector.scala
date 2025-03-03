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

package v6.lossClaims.delete

import shared.connectors.DownstreamUri.{DesUri, HipUri}
import shared.connectors.httpparsers.StandardDownstreamHttpParser._
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import shared.config.{SharedAppConfig, ConfigFeatureSwitches}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v6.lossClaims.delete.model.request.DeleteLossClaimRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteLossClaimConnector @Inject() (val http: HttpClient, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def deleteLossClaim(request: DeleteLossClaimRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[Unit]] = {

    import request._

    val downstreamUri = if (ConfigFeatureSwitches().isEnabled("des_hip_migration_1509")) {
      HipUri[Unit](s"itsa/income-tax/v1/claims-for-relief/$nino/${taxYearClaimedFor.asTysDownstream}/$claimId")
    } else {
      DesUri[Unit](s"income-tax/claims-for-relief/$nino/$claimId")
    }

    delete(downstreamUri)
  }

}
