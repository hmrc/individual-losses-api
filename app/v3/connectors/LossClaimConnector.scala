/*
 * Copyright 2021 HM Revenue & Customs
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

package v3.connectors

import config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v3.connectors.DownstreamUri.IfsUri
import v3.connectors.httpparsers.StandardDownstreamHttpParser._
import v3.models.downstream.{CreateLossClaimResponse, ListLossClaimsResponse, LossClaimId, LossClaimResponse}
import v3.models.requestData._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LossClaimConnector @Inject()(val http: HttpClient,
                                   val appConfig: AppConfig) extends BaseDownstreamConnector {

  def createLossClaim(request: CreateLossClaimRequest)(implicit hc: HeaderCarrier,
                                                       ec: ExecutionContext): Future[DownstreamOutcome[CreateLossClaimResponse]] = {
    val nino = request.nino.nino

    post(request.lossClaim, IfsUri[CreateLossClaimResponse](s"income-tax/claims-for-relief/$nino"))
  }

  def amendLossClaim(amendLossClaimRequest: AmendLossClaimRequest)(implicit hc: HeaderCarrier,
                                                                   ec: ExecutionContext): Future[DownstreamOutcome[LossClaimResponse]] = {
    val nino    = amendLossClaimRequest.nino.nino
    val claimId = amendLossClaimRequest.claimId

    put(amendLossClaimRequest.amendLossClaim, IfsUri[LossClaimResponse](s"income-tax/claims-for-relief/$nino/$claimId"))
  }

  def retrieveLossClaim(request: RetrieveLossClaimRequest)(implicit hc: HeaderCarrier,
                                                           ec: ExecutionContext): Future[DownstreamOutcome[LossClaimResponse]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    get(IfsUri[LossClaimResponse](s"income-tax/claims-for-relief/$nino/$claimId"))
  }

  def listLossClaims(request: ListLossClaimsRequest)(implicit hc: HeaderCarrier,
                                                     ec: ExecutionContext): Future[DownstreamOutcome[ListLossClaimsResponse[LossClaimId]]] = {
    val nino = request.nino.nino
    val pathParameters = Map(
      "taxYear"          -> request.taxYear.map(_.value),
      "incomeSourceId"   -> request.businessId,
      "incomeSourceType" -> request.incomeSourceType.map(_.toString),
      "claimType"        -> request.claimType.map(_.toString)
    ).collect {
        case (key, Some(value)) => key -> value
    }

    get(IfsUri[ListLossClaimsResponse[LossClaimId]](s"income-tax/claims-for-relief/$nino"), pathParameters.toSeq)
  }

  def deleteLossClaim(request: DeleteLossClaimRequest)(implicit hc: HeaderCarrier,
                                                       ec: ExecutionContext): Future[DownstreamOutcome[Unit]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    delete(IfsUri[Unit](s"income-tax/claims-for-relief/$nino/$claimId"))
  }

  def amendLossClaimsOrder(request: AmendLossClaimsOrderRequest)(implicit hc: HeaderCarrier,
                                                                 ec: ExecutionContext): Future[DownstreamOutcome[Unit]] = {
    val nino    = request.nino.nino
    val taxYear = request.taxYear

    put(request.body, IfsUri[Unit](s"income-tax/claims-for-relief/$nino/preferences/$taxYear"))
  }
}