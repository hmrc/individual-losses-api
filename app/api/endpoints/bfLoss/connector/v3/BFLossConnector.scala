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

package api.endpoints.bfLoss.connector.v3

import api.connectors.DownstreamUri.{ DesUri, IfsUri, TaxYearSpecificIfsUri }
import api.connectors.httpparsers.StandardDownstreamHttpParser._
import api.connectors.{ BaseDownstreamConnector, DownstreamOutcome }
import api.endpoints.bfLoss.amend.v3.request.AmendBFLossRequest
import api.endpoints.bfLoss.amend.v3.response.AmendBFLossResponse
import api.endpoints.bfLoss.create.v3.request.CreateBFLossRequest
import api.endpoints.bfLoss.create.v3.response.CreateBFLossResponse
import api.endpoints.bfLoss.delete.v3.request.DeleteBFLossRequest
import api.endpoints.bfLoss.list.v3.request.ListBFLossesRequest
import api.endpoints.bfLoss.list.v3.response.{ ListBFLossesItem, ListBFLossesResponse }
import api.endpoints.bfLoss.retrieve.v3.request.RetrieveBFLossRequest
import api.endpoints.bfLoss.retrieve.v3.response.RetrieveBFLossResponse
import config.AppConfig
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class BFLossConnector @Inject()(val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def createBFLoss(createBFLossRequest: CreateBFLossRequest)(implicit
                                                             hc: HeaderCarrier,
                                                             ec: ExecutionContext,
                                                             correlationId: String): Future[DownstreamOutcome[CreateBFLossResponse]] = {
    val nino = createBFLossRequest.nino.nino

    post(createBFLossRequest.broughtForwardLoss, IfsUri[CreateBFLossResponse](s"income-tax/brought-forward-losses/$nino"))
  }

  def amendBFLoss(amendBFLossRequest: AmendBFLossRequest)(implicit
                                                          hc: HeaderCarrier,
                                                          ec: ExecutionContext,
                                                          correlationId: String): Future[DownstreamOutcome[AmendBFLossResponse]] = {
    val nino   = amendBFLossRequest.nino.nino
    val lossId = amendBFLossRequest.lossId

    put(amendBFLossRequest.amendBroughtForwardLoss, IfsUri[AmendBFLossResponse](s"income-tax/brought-forward-losses/$nino/$lossId"))
  }

  def deleteBFLoss(request: DeleteBFLossRequest)(implicit
                                                 hc: HeaderCarrier,
                                                 ec: ExecutionContext,
                                                 correlationId: String): Future[DownstreamOutcome[Unit]] = {
    val nino   = request.nino.nino
    val lossId = request.lossId

    delete(DesUri[Unit](s"income-tax/brought-forward-losses/$nino/$lossId"))
  }

  def retrieveBFLoss(request: RetrieveBFLossRequest)(implicit
                                                     hc: HeaderCarrier,
                                                     ec: ExecutionContext,
                                                     correlationId: String): Future[DownstreamOutcome[RetrieveBFLossResponse]] = {
    val nino   = request.nino.nino
    val lossId = request.lossId

    get(IfsUri[RetrieveBFLossResponse](s"income-tax/brought-forward-losses/$nino/$lossId"))
  }

  def listBFLosses(request: ListBFLossesRequest)(implicit
                                                 hc: HeaderCarrier,
                                                 ec: ExecutionContext,
                                                 correlationId: String): Future[DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]]] = {
    import request._

    val queryParams = List(
      businessId.map("incomeSourceId"         -> _),
      incomeSourceType.map("incomeSourceType" -> _.toString)
    ).flatten

    taxYearBroughtForwardFrom match {
      case Some(taxYear) if taxYear.useTaxYearSpecificApi =>
        get(
          TaxYearSpecificIfsUri[ListBFLossesResponse[ListBFLossesItem]](s"income-tax/brought-forward-losses/${taxYear.asTysDownstream}/${nino.nino}"),
          queryParams
        )
      case _ =>
        val params = taxYearBroughtForwardFrom match {
          case Some(taxYear) => queryParams :+ "taxYear" -> taxYear.asDownstream
          case None          => queryParams
        }

        get(
          IfsUri[ListBFLossesResponse[ListBFLossesItem]](s"income-tax/brought-forward-losses/${nino.nino}"),
          params
        )
    }

  }
}
