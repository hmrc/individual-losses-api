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

package api.connectors.v2

import api.connectors.httpparsers.StandardDownstreamHttpParser._
import api.connectors.{ BaseDownstreamConnector, DownstreamOutcome }
import api.models.domain.v2.{ AmendLossClaim, AmendLossClaimsOrderRequestBody, LossClaim }
import config.AppConfig
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient }
import v2.models.des.{ CreateLossClaimResponse, ListLossClaimsResponse, LossClaimId, LossClaimResponse }
import v2.models.requestData._

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class LossClaimConnector @Inject()(val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def createLossClaim(request: CreateLossClaimRequest)(implicit hc: HeaderCarrier,
                                                       ec: ExecutionContext): Future[DownstreamOutcome[CreateLossClaimResponse]] = {
    val nino = request.nino.nino

    def doIt(implicit hc: HeaderCarrier): Future[DownstreamOutcome[CreateLossClaimResponse]] =
      http
        .POST[LossClaim, DownstreamOutcome[CreateLossClaimResponse]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino", request.lossClaim)

    doIt(desHeaderCarrier(Seq("Content-Type")))
  }

  def amendLossClaim(amendLossClaimRequest: AmendLossClaimRequest)(implicit hc: HeaderCarrier,
                                                                   ec: ExecutionContext): Future[DownstreamOutcome[LossClaimResponse]] = {
    val nino    = amendLossClaimRequest.nino.nino
    val claimId = amendLossClaimRequest.claimId

    def doIt(implicit hc: HeaderCarrier): Future[DownstreamOutcome[LossClaimResponse]] =
      http.PUT[AmendLossClaim, DownstreamOutcome[LossClaimResponse]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino/$claimId",
                                                                     amendLossClaimRequest.amendLossClaim)

    doIt(desHeaderCarrier(Seq("Content-Type")))
  }

  def retrieveLossClaim(request: RetrieveLossClaimRequest)(implicit hc: HeaderCarrier,
                                                           ec: ExecutionContext): Future[DownstreamOutcome[LossClaimResponse]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    def doIt(implicit hc: HeaderCarrier): Future[DownstreamOutcome[LossClaimResponse]] = {
      http.GET[DownstreamOutcome[LossClaimResponse]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino/$claimId")
    }

    doIt(desHeaderCarrier())
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

    def doIt(implicit hc: HeaderCarrier): Future[DownstreamOutcome[ListLossClaimsResponse[LossClaimId]]] = {
      http.GET[DownstreamOutcome[ListLossClaimsResponse[LossClaimId]]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino",
                                                                       pathParameters.toSeq)
    }

    doIt(desHeaderCarrier())
  }

  def deleteLossClaim(request: DeleteLossClaimRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DownstreamOutcome[Unit]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    def doIt(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Unit]] =
      http.DELETE[DownstreamOutcome[Unit]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino/$claimId")

    doIt(desHeaderCarrier())
  }

  def amendLossClaimsOrder(request: AmendLossClaimsOrderRequest)(implicit hc: HeaderCarrier,
                                                                 ec: ExecutionContext): Future[DownstreamOutcome[Unit]] = {
    val nino    = request.nino.nino
    val taxYear = request.taxYear

    def doIt(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Unit]] =
      http.PUT[AmendLossClaimsOrderRequestBody, DownstreamOutcome[Unit]](
        s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino/preferences/$taxYear",
        request.body)

    doIt(desHeaderCarrier(Seq("Content-Type")))
  }
}
