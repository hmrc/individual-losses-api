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
import v3.connectors.httpparsers.StandardDownstreamHttpParser._
import v3.models.downstream.{BFLossId, BFLossResponse, CreateBFLossResponse, ListBFLossesResponse}
import v3.models.domain.{AmendBFLoss, BFLoss}
import v3.models.requestData._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BFLossConnector @Inject()(val http: HttpClient,
                                val appConfig: AppConfig) extends BaseIfsConnector {

  def createBFLoss(createBFLossRequest: CreateBFLossRequest)(implicit hc: HeaderCarrier,
                                                             ec: ExecutionContext): Future[IfsOutcome[CreateBFLossResponse]] = {
    val nino = createBFLossRequest.nino.nino

    def doIt(implicit hc: HeaderCarrier): Future[IfsOutcome[CreateBFLossResponse]] =
      http.POST[BFLoss, IfsOutcome[CreateBFLossResponse]](s"${appConfig.ifsBaseUrl}/income-tax/brought-forward-losses/$nino",
        createBFLossRequest.broughtForwardLoss)

    doIt(ifsHeaderCarrier(Seq("Content-Type")))
  }

  def amendBFLoss(amendBFLossRequest: AmendBFLossRequest)(implicit hc: HeaderCarrier,
                                                          ec: ExecutionContext): Future[IfsOutcome[BFLossResponse]] = {
    val nino = amendBFLossRequest.nino.nino
    val lossId = amendBFLossRequest.lossId

    def doIt(implicit hc: HeaderCarrier): Future[IfsOutcome[BFLossResponse]] =
      http.PUT[AmendBFLoss, IfsOutcome[BFLossResponse]](s"${appConfig.ifsBaseUrl}/income-tax/brought-forward-losses/$nino/$lossId",
        amendBFLossRequest.amendBroughtForwardLoss)

    doIt(ifsHeaderCarrier(Seq("Content-Type")))
  }

  def deleteBFLoss(request: DeleteBFLossRequest)(implicit hc: HeaderCarrier,
                                                 ec: ExecutionContext): Future[IfsOutcome[Unit]] = {
    val nino = request.nino.nino
    val lossId = request.lossId

    def doIt(implicit hc: HeaderCarrier): Future[IfsOutcome[Unit]] =
      http.DELETE[IfsOutcome[Unit]](s"${appConfig.ifsBaseUrl}/income-tax/brought-forward-losses/$nino/$lossId")

    doIt(ifsHeaderCarrier())
  }

  def retrieveBFLoss(request: RetrieveBFLossRequest)(implicit hc: HeaderCarrier,
                                                     ec: ExecutionContext): Future[IfsOutcome[BFLossResponse]] = {
    val nino = request.nino.nino
    val lossId = request.lossId

    def doIt(implicit hc: HeaderCarrier): Future[IfsOutcome[BFLossResponse]] = {
      http.GET[IfsOutcome[BFLossResponse]](s"${appConfig.ifsBaseUrl}/income-tax/brought-forward-losses/$nino/$lossId")
    }

    doIt(ifsHeaderCarrier())
  }

  def listBFLosses(request: ListBFLossesRequest)(implicit hc: HeaderCarrier,
                                                 ec: ExecutionContext): Future[IfsOutcome[ListBFLossesResponse[BFLossId]]] = {
    val nino = request.nino.nino
    val pathParameters = Map(
      "taxYear"          -> request.taxYear.map(_.value),
      "incomeSourceId"   -> request.businessId,
      "incomeSourceType" -> request.incomeSourceType.map(_.toString)
    ).collect {
      case (key, Some(value)) => key -> value
    }

    def doIt(implicit hc: HeaderCarrier): Future[IfsOutcome[ListBFLossesResponse[BFLossId]]] = {
      http.GET[IfsOutcome[ListBFLossesResponse[BFLossId]]](s"${appConfig.ifsBaseUrl}/income-tax/brought-forward-losses/$nino", pathParameters.toSeq)
    }

    doIt(ifsHeaderCarrier())
  }
}