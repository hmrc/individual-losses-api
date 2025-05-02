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

package v4.connectors

import shared.connectors.DownstreamUri.IfsUri
import shared.connectors.httpparsers.StandardDownstreamHttpParser._
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import shared.config.SharedAppConfig
import shared.models.domain.TaxYear
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v4.models.request.amendLossClaimType.AmendLossClaimTypeRequestData
import v4.models.response.amendLossClaimType.AmendLossClaimTypeResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendLossClaimTypeConnector @Inject() (val http: HttpClient, val appConfig: SharedAppConfig) extends BaseDownstreamConnector {

  def amendLossClaimType(request: AmendLossClaimTypeRequestData, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String
  ): Future[DownstreamOutcome[AmendLossClaimTypeResponse]] = {

    import request._

    put(amendLossClaimTypeRequestBody, IfsUri[AmendLossClaimTypeResponse](s"income-tax/claims-for-relief/$nino/${taxYear.asTysDownstream}/$claimId"))
  }

}
