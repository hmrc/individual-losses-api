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

package v2.connectors

import api.connectors.httpparsers.StandardDownstreamHttpParser._
import api.connectors.{ BaseDownstreamConnector, DownstreamOutcome }
import config.AppConfig
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient }
import v2.models.des.{ BFLossId, BFLossResponse, CreateBFLossResponse, ListBFLossesResponse }
import v2.models.domain.{ AmendBFLoss, BFLoss }
import v2.models.requestData._

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class BFLossConnector @Inject()(val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def createBFLoss(createBFLossRequest: CreateBFLossRequest)(implicit hc: HeaderCarrier,
                                                             ec: ExecutionContext): Future[DownstreamOutcome[CreateBFLossResponse]] = {
    val nino = createBFLossRequest.nino.nino

    def doIt(implicit hc: HeaderCarrier): Future[DownstreamOutcome[CreateBFLossResponse]] =
      http.POST[BFLoss, DownstreamOutcome[CreateBFLossResponse]](s"${appConfig.desBaseUrl}/income-tax/brought-forward-losses/$nino",
                                                                 createBFLossRequest.broughtForwardLoss)

    doIt(desHeaderCarrier(Seq("Content-Type")))
  }

  def amendBFLoss(amendBFLossRequest: AmendBFLossRequest)(implicit hc: HeaderCarrier,
                                                          ec: ExecutionContext): Future[DownstreamOutcome[BFLossResponse]] = {
    val nino   = amendBFLossRequest.nino.nino
    val lossId = amendBFLossRequest.lossId

    def doIt(implicit hc: HeaderCarrier): Future[DownstreamOutcome[BFLossResponse]] =
      http.PUT[AmendBFLoss, DownstreamOutcome[BFLossResponse]](s"${appConfig.desBaseUrl}/income-tax/brought-forward-losses/$nino/$lossId",
                                                               amendBFLossRequest.amendBroughtForwardLoss)

    doIt(desHeaderCarrier(Seq("Content-Type")))
  }

  def deleteBFLoss(request: DeleteBFLossRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DownstreamOutcome[Unit]] = {
    val nino   = request.nino.nino
    val lossId = request.lossId

    def doIt(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Unit]] =
      http.DELETE[DownstreamOutcome[Unit]](s"${appConfig.desBaseUrl}/income-tax/brought-forward-losses/$nino/$lossId")

    doIt(desHeaderCarrier())
  }

  def retrieveBFLoss(request: RetrieveBFLossRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DownstreamOutcome[BFLossResponse]] = {
    val nino   = request.nino.nino
    val lossId = request.lossId

    def doIt(implicit hc: HeaderCarrier): Future[DownstreamOutcome[BFLossResponse]] = {
      http.GET[DownstreamOutcome[BFLossResponse]](s"${appConfig.desBaseUrl}/income-tax/brought-forward-losses/$nino/$lossId")
    }

    doIt(desHeaderCarrier())
  }

  def listBFLosses(request: ListBFLossesRequest)(implicit hc: HeaderCarrier,
                                                 ec: ExecutionContext): Future[DownstreamOutcome[ListBFLossesResponse[BFLossId]]] = {
    val nino = request.nino.nino
    val pathParameters = Map(
      "taxYear"          -> request.taxYear.map(_.value),
      "incomeSourceId"   -> request.businessId,
      "incomeSourceType" -> request.incomeSourceType.map(_.toString)
    ).collect {
      case (key, Some(value)) => key -> value
    }

    def doIt(implicit hc: HeaderCarrier): Future[DownstreamOutcome[ListBFLossesResponse[BFLossId]]] = {
      http.GET[DownstreamOutcome[ListBFLossesResponse[BFLossId]]](s"${appConfig.desBaseUrl}/income-tax/brought-forward-losses/$nino",
                                                                  pathParameters.toSeq)
    }

    doIt(desHeaderCarrier())
  }
}
