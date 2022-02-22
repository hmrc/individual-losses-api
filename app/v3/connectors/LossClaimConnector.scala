/*
 * Copyright 2022 HM Revenue & Customs
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
import v3.connectors.DownstreamUri.{DesUri, IfsUri}
import v3.connectors.httpparsers.StandardDownstreamHttpParser._
import v3.models.request.amendLossClaimType.AmendLossClaimTypeRequest
import v3.models.request.amendLossClaimsOrder.AmendLossClaimsOrderRequest
import v3.models.request.createLossClaim.CreateLossClaimRequest
import v3.models.request.deleteLossClaim.DeleteLossClaimRequest
import v3.models.request.listLossClaims.ListLossClaimsRequest
import v3.models.request.retrieveLossClaim.RetrieveLossClaimRequest
import v3.models.response.amendLossClaimType.AmendLossClaimTypeResponse
import v3.models.response.createLossClaim.CreateLossClaimResponse
import v3.models.response.listLossClaims.{ListLossClaimsItem, ListLossClaimsResponse}
import v3.models.response.retrieveLossClaim.RetrieveLossClaimResponse

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

  def amendLossClaimType(amendLossClaimTypeRequest: AmendLossClaimTypeRequest)(implicit hc: HeaderCarrier,
                                                                               ec: ExecutionContext): Future[DownstreamOutcome[AmendLossClaimTypeResponse]] = {
    val nino    = amendLossClaimTypeRequest.nino.nino
    val claimId = amendLossClaimTypeRequest.claimId

    put(amendLossClaimTypeRequest.amendLossClaimType, IfsUri[AmendLossClaimTypeResponse](s"income-tax/claims-for-relief/$nino/$claimId"))
  }

  def retrieveLossClaim(request: RetrieveLossClaimRequest)(implicit hc: HeaderCarrier,
                                                           ec: ExecutionContext): Future[DownstreamOutcome[RetrieveLossClaimResponse]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    get(IfsUri[RetrieveLossClaimResponse](s"income-tax/claims-for-relief/$nino/$claimId"))
  }

  def listLossClaims(request: ListLossClaimsRequest)(implicit hc: HeaderCarrier,
                                                     ec: ExecutionContext): Future[DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]]] = {
    val nino = request.nino.nino
    val pathParameters = Map(
      "taxYear" -> request.taxYearClaimedFor.map(_.value),
      "incomeSourceId"    -> request.businessId,
      "incomeSourceType"  -> request.typeOfLoss.flatMap(_.toIncomeSourceType).map(_.toString),
      "claimType"         -> request.typeOfClaim.map(_.toReliefClaimed.toString)
    ).collect {
        case (key, Some(value)) => key -> value
    }
    get(IfsUri[ListLossClaimsResponse[ListLossClaimsItem]](s"income-tax/claims-for-relief/$nino"), pathParameters.toSeq)
  }

  def deleteLossClaim(request: DeleteLossClaimRequest)(implicit hc: HeaderCarrier,
                                                       ec: ExecutionContext): Future[DownstreamOutcome[Unit]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    delete(DesUri[Unit](s"income-tax/claims-for-relief/$nino/$claimId"))
  }

  def amendLossClaimsOrder(request: AmendLossClaimsOrderRequest)(implicit hc: HeaderCarrier,
                                                                 ec: ExecutionContext): Future[DownstreamOutcome[Unit]] = {
    val nino    = request.nino.nino
    val taxYear = request.taxYearClaimedFor

    put(request.body, DesUri[Unit](s"income-tax/claims-for-relief/$nino/preferences/$taxYear"))
  }
}