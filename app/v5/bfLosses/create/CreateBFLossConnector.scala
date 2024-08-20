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

package v5.bfLosses.create

import api.connectors.{BaseDownstreamConnector, DownstreamOutcome, DownstreamUri}
import api.connectors.DownstreamUri.IfsUri
import config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v5.bfLosses.create.model.request.CreateBFLossRequestData
import v5.bfLosses.create.model.response.CreateBFLossResponse
import api.connectors.httpparsers.StandardDownstreamHttpParser.reads

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateBFLossConnector @Inject() (val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def createBFLoss(request: CreateBFLossRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[CreateBFLossResponse]] = {
    import request._
    import schema._
    val downstreamUri: DownstreamUri[DownstreamResp] = IfsUri(s"income-tax/brought-forward-losses/$nino")

    post(broughtForwardLoss, downstreamUri)
  }

}
