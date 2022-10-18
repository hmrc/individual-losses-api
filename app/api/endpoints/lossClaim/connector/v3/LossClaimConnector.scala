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

package api.endpoints.lossClaim.connector.v3

import api.connectors.DownstreamUri.{DesUri, IfsUri, TaxYearSpecificIfsUri}
import api.connectors.httpparsers.StandardDownstreamHttpParser._
import api.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import api.endpoints.lossClaim.amendOrder.v3.request.AmendLossClaimsOrderRequest
import api.endpoints.lossClaim.amendType.v3.request.AmendLossClaimTypeRequest
import api.endpoints.lossClaim.amendType.v3.response.AmendLossClaimTypeResponse
import api.endpoints.lossClaim.create.v3.request.CreateLossClaimRequest
import api.endpoints.lossClaim.create.v3.response.CreateLossClaimResponse
import api.endpoints.lossClaim.delete.v3.request.DeleteLossClaimRequest
import api.endpoints.lossClaim.list.v3.request.ListLossClaimsRequest
import api.endpoints.lossClaim.list.v3.response.{ListLossClaimsItem, ListLossClaimsResponse}
import api.endpoints.lossClaim.retrieve.v3.request.RetrieveLossClaimRequest
import api.endpoints.lossClaim.retrieve.v3.response.RetrieveLossClaimResponse
import config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LossClaimConnector @Inject()(val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def createLossClaim(request: CreateLossClaimRequest)(implicit
                                                       hc: HeaderCarrier,
                                                       ec: ExecutionContext,
                                                       correlationId: String): Future[DownstreamOutcome[CreateLossClaimResponse]] = {
    val nino = request.nino.nino

    post(request.lossClaim, IfsUri[CreateLossClaimResponse](s"income-tax/claims-for-relief/$nino"))
  }

  def amendLossClaimType(amendLossClaimTypeRequest: AmendLossClaimTypeRequest)(implicit
                                                                               hc: HeaderCarrier,
                                                                               ec: ExecutionContext,
                                                                               correlationId: String): Future[DownstreamOutcome[AmendLossClaimTypeResponse]] = {
    val nino    = amendLossClaimTypeRequest.nino.nino
    val claimId = amendLossClaimTypeRequest.claimId

    put(amendLossClaimTypeRequest.amendLossClaimType, IfsUri[AmendLossClaimTypeResponse](s"income-tax/claims-for-relief/$nino/$claimId"))
  }

  def retrieveLossClaim(request: RetrieveLossClaimRequest)(implicit
                                                           hc: HeaderCarrier,
                                                           ec: ExecutionContext,
                                                           correlationId: String): Future[DownstreamOutcome[RetrieveLossClaimResponse]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    get(IfsUri[RetrieveLossClaimResponse](s"income-tax/claims-for-relief/$nino/$claimId"))
  }

  def listLossClaims(request: ListLossClaimsRequest)(implicit
                                                     hc: HeaderCarrier,
                                                     ec: ExecutionContext,
                                                     correlationId: String): Future[DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]]] = {
    val nino = request.nino.nino
    val pathParameters = Map(
      "taxYear"          -> request.taxYearClaimedFor.map(_.value),
      "incomeSourceId"   -> request.businessId,
      "incomeSourceType" -> request.typeOfLoss.flatMap(_.toIncomeSourceType).map(_.toString),
      "claimType"        -> request.typeOfClaim.map(_.toReliefClaimed.toString)
    ).collect {
      case (key, Some(value)) => key -> value
    }

    get(IfsUri[ListLossClaimsResponse[ListLossClaimsItem]](s"income-tax/claims-for-relief/$nino"), pathParameters.toSeq)
  }

  def deleteLossClaim(request: DeleteLossClaimRequest)(implicit hc: HeaderCarrier,
                                                       ec: ExecutionContext,
                                                       correlationId: String): Future[DownstreamOutcome[Unit]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    delete(DesUri[Unit](s"income-tax/claims-for-relief/$nino/$claimId"))
  }

  def amendLossClaimsOrder(request: AmendLossClaimsOrderRequest)(implicit hc: HeaderCarrier,
                                                                 ec: ExecutionContext,
                                                                 correlationId: String): Future[DownstreamOutcome[Unit]] = {
    val nino    = request.nino.nino
    val taxYear = request.taxYearClaimedFor

    val downstreamUri =
      if (taxYear.useTaxYearSpecificApi) {
        TaxYearSpecificIfsUri[Unit](s"income-tax/claims-for-relief/preferences/${taxYear.asTysDownstream}/$nino")
      } else {
        DesUri[Unit](s"income-tax/claims-for-relief/$nino/preferences/${taxYear.asDownstream}")
      }

    put(
      uri = downstreamUri,
      body = request.body
    )
  }
}
