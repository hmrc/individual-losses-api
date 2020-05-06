/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.connectors

import config.AppConfig
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import v1.connectors.httpparsers.StandardDesHttpParser._
import v1.models.des.{CreateLossClaimResponse, ListLossClaimsResponse, LossClaimId, LossClaimResponse}
import v1.models.domain.{AmendLossClaim, LossClaim, LossClaimsList}
import v1.models.requestData._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LossClaimConnector @Inject()(http: HttpClient, appConfig: AppConfig) extends DesConnector {

  def createLossClaim(request: CreateLossClaimRequest)(implicit hc: HeaderCarrier,
                                                       ec: ExecutionContext): Future[DesOutcome[CreateLossClaimResponse]] = {
    val nino = request.nino.nino

    def doIt(implicit hc: HeaderCarrier) =
      http.POST[LossClaim, DesOutcome[CreateLossClaimResponse]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino", request.lossClaim)

    doIt(desHeaderCarrier(appConfig))
  }

  def amendLossClaim(amendLossClaimRequest: AmendLossClaimRequest)(implicit hc: HeaderCarrier,
                                                                   ec: ExecutionContext): Future[DesOutcome[LossClaimResponse]] = {

    val nino    = amendLossClaimRequest.nino.nino
    val claimId = amendLossClaimRequest.claimId

    def doIt(implicit hc: HeaderCarrier) =
      http.PUT[AmendLossClaim, DesOutcome[LossClaimResponse]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino/$claimId",
                                                              amendLossClaimRequest.amendLossClaim)

    doIt(desHeaderCarrier(appConfig))
  }

  def retrieveLossClaim(request: RetrieveLossClaimRequest)(implicit hc: HeaderCarrier,
                                                           ec: ExecutionContext): Future[DesOutcome[LossClaimResponse]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    def doIt(implicit hc: HeaderCarrier): Future[DesOutcome[LossClaimResponse]] = {
      http.GET[DesOutcome[LossClaimResponse]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino/$claimId")
    }

    doIt(desHeaderCarrier(appConfig))
  }

  def listLossClaims(request: ListLossClaimsRequest)(implicit hc: HeaderCarrier,
                                                     ec: ExecutionContext): Future[DesOutcome[ListLossClaimsResponse[LossClaimId]]] = {

    val nino = request.nino.nino
    val pathParameters =
      Map(
        "taxYear"          -> request.taxYear.map(_.value),
        "incomeSourceId"   -> request.selfEmploymentId,
        "incomeSourceType" -> request.incomeSourceType.map(_.toString),
        "claimType"        -> request.claimType.map(_.toString)
      ).collect {
        case (key, Some(value)) => key -> value
      }

    def doIt(implicit hc: HeaderCarrier): Future[DesOutcome[ListLossClaimsResponse[LossClaimId]]] = {
      http.GET[DesOutcome[ListLossClaimsResponse[LossClaimId]]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino", pathParameters.toSeq)
    }

    doIt(desHeaderCarrier(appConfig))
  }

  def deleteLossClaim(request: DeleteLossClaimRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DesOutcome[Unit]] = {
    val nino    = request.nino.nino
    val claimId = request.claimId

    def doIt(implicit hc: HeaderCarrier) =
      http.DELETE[DesOutcome[Unit]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino/$claimId")

    doIt(desHeaderCarrier(appConfig))
  }

  def amendLossClaimsOrder(request: AmendLossClaimsOrderRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DesOutcome[Unit]] = {
    val nino    = request.nino.nino
    val taxYear = request.taxYear

    def doIt(implicit hc: HeaderCarrier) =
      http.PUT[LossClaimsList, DesOutcome[Unit]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino/preferences/$taxYear", request.body)

    doIt(desHeaderCarrier(appConfig))
  }
}
