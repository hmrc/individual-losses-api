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

import api.connectors.DownstreamOutcome
import api.connectors.httpparsers.StandardDownstreamHttpParser._
import config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v3.connectors.DownstreamUri.{DesUri, IfsUri}
import v3.models.request.amendBFLoss.AmendBFLossRequest
import v3.models.request.createBFLoss.CreateBFLossRequest
import v3.models.request.deleteBFLoss.DeleteBFLossRequest
import v3.models.request.listBFLosses.ListBFLossesRequest
import v3.models.request.retrieveBFLoss.RetrieveBFLossRequest
import v3.models.response.amendBFLoss.AmendBFLossResponse
import v3.models.response.createBFLoss.CreateBFLossResponse
import v3.models.response.listBFLosses.{ListBFLossesItem, ListBFLossesResponse}
import v3.models.response.retrieveBFLoss.RetrieveBFLossResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BFLossConnector @Inject()(val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def createBFLoss(createBFLossRequest: CreateBFLossRequest)(implicit hc: HeaderCarrier,
                                                             ec: ExecutionContext): Future[DownstreamOutcome[CreateBFLossResponse]] = {
    val nino = createBFLossRequest.nino.nino

    post(createBFLossRequest.broughtForwardLoss, IfsUri[CreateBFLossResponse](s"income-tax/brought-forward-losses/$nino"))
  }

  def amendBFLoss(amendBFLossRequest: AmendBFLossRequest)(implicit hc: HeaderCarrier,
                                                          ec: ExecutionContext): Future[DownstreamOutcome[AmendBFLossResponse]] = {
    val nino   = amendBFLossRequest.nino.nino
    val lossId = amendBFLossRequest.lossId

    put(amendBFLossRequest.amendBroughtForwardLoss, IfsUri[AmendBFLossResponse](s"income-tax/brought-forward-losses/$nino/$lossId"))
  }

  def deleteBFLoss(request: DeleteBFLossRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DownstreamOutcome[Unit]] = {
    val nino   = request.nino.nino
    val lossId = request.lossId

    delete(DesUri[Unit](s"income-tax/brought-forward-losses/$nino/$lossId"))
  }

  def retrieveBFLoss(request: RetrieveBFLossRequest)(implicit hc: HeaderCarrier,
                                                     ec: ExecutionContext): Future[DownstreamOutcome[RetrieveBFLossResponse]] = {
    val nino   = request.nino.nino
    val lossId = request.lossId

    get(IfsUri[RetrieveBFLossResponse](s"income-tax/brought-forward-losses/$nino/$lossId"))
  }

  def listBFLosses(request: ListBFLossesRequest)(implicit hc: HeaderCarrier,
                                                 ec: ExecutionContext): Future[DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]]] = {
    val nino = request.nino.nino
    val pathParameters = Map(
      "taxYear"          -> request.taxYearBroughtForwardFrom.map(_.value),
      "incomeSourceId"   -> request.businessId,
      "incomeSourceType" -> request.incomeSourceType.map(_.toString)
    ).collect {
      case (key, Some(value)) => key -> value
    }

    get(IfsUri[ListBFLossesResponse[ListBFLossesItem]](s"income-tax/brought-forward-losses/$nino"), pathParameters.toSeq)
  }
}
