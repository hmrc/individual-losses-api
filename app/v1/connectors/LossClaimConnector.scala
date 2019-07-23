/*
 * Copyright 2019 HM Revenue & Customs
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
import v1.models.des.{AmendLossClaimResponse, CreateLossClaimResponse, ListLossClaimsResponse, RetrieveLossClaimResponse}
import v1.models.domain.{AmendLossClaim, LossClaim}
import v1.models.requestData._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LossClaimConnector @Inject()(http: HttpClient, appConfig: AppConfig) extends DesConnector {

  def createLossClaim(request: CreateLossClaimRequest)(implicit hc: HeaderCarrier,
                                                       ec: ExecutionContext): Future[DesOutcome[CreateLossClaimResponse]] = {
    val nino = request.nino.nino

    def doIt(implicit hc: HeaderCarrier) =
      http.POST[LossClaim, DesOutcome[CreateLossClaimResponse]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino",
        request.lossClaim)

    doIt(desHeaderCarrier(appConfig))
  }

  def amendLossClaim(amendLossClaimRequest: AmendLossClaimRequest)(implicit hc: HeaderCarrier,
                                                                   ec: ExecutionContext): Future[DesOutcome[AmendLossClaimResponse]] = {

    val nino = amendLossClaimRequest.nino.nino
    val claimId = amendLossClaimRequest.claimId

    def doIt(implicit hc: HeaderCarrier) =
      http.PUT[AmendLossClaim, DesOutcome[AmendLossClaimResponse]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino/$claimId",
        amendLossClaimRequest.amendLossClaim)

    doIt(desHeaderCarrier(appConfig))
  }

  def retrieveLossClaim(request: RetrieveLossClaimRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DesOutcome[RetrieveLossClaimResponse]] = {
    val nino = request.nino.nino
    val claimId = request.claimId

    def doIt(implicit hc: HeaderCarrier): Future[DesOutcome[RetrieveLossClaimResponse]] = {
      http.GET[DesOutcome[RetrieveLossClaimResponse]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino/$claimId")
    }

    doIt(desHeaderCarrier(appConfig))
  }

  def listLossClaims(request: ListLossClaimsRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DesOutcome[ListLossClaimsResponse]] = {
    val nino = request.nino.nino

    def doIt(implicit hc: HeaderCarrier): Future[DesOutcome[ListLossClaimsResponse]] = {
      http.GET[DesOutcome[ListLossClaimsResponse]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino")
    }

    doIt(desHeaderCarrier(appConfig))
  }

  def deleteLossClaim(request: DeleteLossClaimRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DesOutcome[Unit]] = {
    val nino = request.nino.nino
    val claimId = request.claimId

    def doIt(implicit hc: HeaderCarrier) =
      http.DELETE[DesOutcome[Unit]](s"${appConfig.desBaseUrl}/income-tax/claims-for-relief/$nino/$claimId")

    doIt(desHeaderCarrier(appConfig))
  }
}
