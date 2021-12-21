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
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient }
import v3.connectors.DownstreamUri.IfsUri
import v3.connectors.httpparsers.StandardDownstreamHttpParser._
import v3.models.downstream.{ BFLossId, BFLossResponse, CreateBFLossResponse, ListBFLossesResponse }
import v3.models.requestData._

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class BFLossConnector @Inject()(val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def createBFLoss(createBFLossRequest: CreateBFLossRequest)(implicit hc: HeaderCarrier,
                                                             ec: ExecutionContext): Future[DownstreamOutcome[CreateBFLossResponse]] = {
    val nino = createBFLossRequest.nino.nino

    post(createBFLossRequest.broughtForwardLoss, IfsUri[CreateBFLossResponse](s"income-tax/brought-forward-losses/$nino"))
  }

  def amendBFLoss(amendBFLossRequest: AmendBFLossRequest)(implicit hc: HeaderCarrier,
                                                          ec: ExecutionContext): Future[DownstreamOutcome[BFLossResponse]] = {
    val nino = amendBFLossRequest.nino.nino
    val lossId = amendBFLossRequest.lossId

    put(amendBFLossRequest.amendBroughtForwardLoss, IfsUri[BFLossResponse](s"income-tax/brought-forward-losses/$nino/$lossId"))
  }

  def deleteBFLoss(request: DeleteBFLossRequest)(implicit hc: HeaderCarrier,
                                                 ec: ExecutionContext): Future[DownstreamOutcome[Unit]] = {
    val nino = request.nino.nino
    val lossId = request.lossId

    delete(IfsUri[Unit](s"income-tax/brought-forward-losses/$nino/$lossId"))
  }

  def retrieveBFLoss(request: RetrieveBFLossRequest)(implicit hc: HeaderCarrier,
                                                     ec: ExecutionContext): Future[DownstreamOutcome[BFLossResponse]] = {
    val nino = request.nino.nino
    val lossId = request.lossId

    get(IfsUri[BFLossResponse](s"income-tax/brought-forward-losses/$nino/$lossId"))
  }

  def listBFLosses(request: ListBFLossesRequest)(implicit hc: HeaderCarrier,
                                                 ec: ExecutionContext): Future[DownstreamOutcome[ListBFLossesResponse[BFLossId]]] = {
    val nino = request.nino.nino
    val pathParameters = Map(
      "taxYear"          -> request.taxYearBroughtForwardFrom.map(_.value),
      "incomeSourceId"   -> request.businessId,
      "incomeSourceType" -> request.incomeSourceType.map(_.toString)
    ).collect {
      case (key, Some(value)) => key -> value
    }

    get(IfsUri[ListBFLossesResponse[BFLossId]](s"income-tax/brought-forward-losses/$nino"), pathParameters.toSeq)
  }
}
