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
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v3.models.domain.AmendLossClaimsOrderRequestBody
import v3.connectors.httpparsers.StandardDownstreamHttpParser._
import v3.models.downstream.{CreateLossClaimResponse, ListLossClaimsResponse, LossClaimId, LossClaimResponse}
import v3.models.domain.{AmendLossClaim, LossClaim}
import v3.models.requestData._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LossClaimConnector @Inject()(val http: HttpClient,
                                   val appConfig: AppConfig) extends BaseIfsConnector {

  def createLossClaim(request: CreateLossClaimRequest)(implicit hc: HeaderCarrier,
                                                       ec: ExecutionContext): Future[IfsOutcome[CreateLossClaimResponse]] = {
    val nino = request.nino.nino

    def doIt(implicit hc: HeaderCarrier): Future[IfsOutcome[CreateLossClaimResponse]] =
      http.POST[LossClaim, IfsOutcome[CreateLossClaimResponse]](s"${appConfig.ifsBaseUrl}/income-tax/claims-for-relief/$nino", request.lossClaim)

    doIt(ifsHeaderCarrier(Seq("Content-Type")))
  }

  def amendLossClaim(amendLossClaimRequest: AmendLossClaimRequest)(implicit hc: HeaderCarrier,
                                                                   ec: ExecutionContext): Future[IfsOutcome[LossClaimResponse]] = {
    val nino    = amendLossClaimRequest.nino.nino
    val claimId = amendLossClaimRequest.claimId

    def doIt(implicit hc: HeaderCarrier): Future[IfsOutcome[LossClaimResponse]] =
      http.PUT[AmendLossClaim, IfsOutcome[LossClaimResponse]](s"${appConfig.ifsBaseUrl}/income-tax/claims-for-relief/$nino/$claimId",
                                                              amendLossClaimRequest.amendLossClaim)

    doIt(ifsHeaderCarrier(Seq("Content-Type")))
  }

  def retrieveLossClaim(request: RetrieveLossClaimRequest)(implicit hc: HeaderCarrier,
                                                           ec: ExecutionContext): Future[IfsOutcome[LossClaimResponse]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    def doIt(implicit hc: HeaderCarrier): Future[IfsOutcome[LossClaimResponse]] = {
      http.GET[IfsOutcome[LossClaimResponse]](s"${appConfig.ifsBaseUrl}/income-tax/claims-for-relief/$nino/$claimId")
    }

    doIt(ifsHeaderCarrier())
  }

  def listLossClaims(request: ListLossClaimsRequest)(implicit hc: HeaderCarrier,
                                                     ec: ExecutionContext): Future[IfsOutcome[ListLossClaimsResponse[LossClaimId]]] = {
    val nino = request.nino.nino
    val pathParameters = Map(
      "taxYear"          -> request.taxYear.map(_.value),
      "incomeSourceId"   -> request.businessId,
      "incomeSourceType" -> request.incomeSourceType.map(_.toString),
      "claimType"        -> request.claimType.map(_.toString)
    ).collect {
        case (key, Some(value)) => key -> value
    }

    def doIt(implicit hc: HeaderCarrier): Future[IfsOutcome[ListLossClaimsResponse[LossClaimId]]] = {
      http.GET[IfsOutcome[ListLossClaimsResponse[LossClaimId]]](s"${appConfig.ifsBaseUrl}/income-tax/claims-for-relief/$nino", pathParameters.toSeq)
    }

    doIt(ifsHeaderCarrier())
  }

  def deleteLossClaim(request: DeleteLossClaimRequest)(implicit hc: HeaderCarrier,
                                                       ec: ExecutionContext): Future[IfsOutcome[Unit]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    def doIt(implicit hc: HeaderCarrier): Future[IfsOutcome[Unit]] =
      http.DELETE[IfsOutcome[Unit]](s"${appConfig.ifsBaseUrl}/income-tax/claims-for-relief/$nino/$claimId")

    doIt(ifsHeaderCarrier())
  }

  def amendLossClaimsOrder(request: AmendLossClaimsOrderRequest)(implicit hc: HeaderCarrier,
                                                                 ec: ExecutionContext): Future[IfsOutcome[Unit]] = {
    val nino    = request.nino.nino
    val taxYear = request.taxYear

    def doIt(implicit hc: HeaderCarrier): Future[IfsOutcome[Unit]] =
      http.PUT[AmendLossClaimsOrderRequestBody,
        IfsOutcome[Unit]](s"${appConfig.ifsBaseUrl}/income-tax/claims-for-relief/$nino/preferences/$taxYear", request.body)

    doIt(ifsHeaderCarrier(Seq("Content-Type")))
  }
}