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
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import v1.connectors.httpparsers.StandardDesHttpParser._
import v1.models.des.{AmendBFLossResponse, CreateBFLossResponse, RetrieveBFLossResponse}
import v1.models.domain.{AmendBFLoss, BFLoss}
import v1.models.requestData.{AmendBFLossRequest, CreateBFLossRequest, DeleteBFLossRequest, RetrieveBFLossRequest}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject()(http: HttpClient, appConfig: AppConfig) {

  val logger = Logger(this.getClass)

  private def desHeaderCarrier(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.copy(authorization = Some(Authorization(s"Bearer ${appConfig.desToken}")))
      .withExtraHeaders("Environment" -> appConfig.desEnv)

  def createBFLoss(createBFLossRequest: CreateBFLossRequest)(implicit hc: HeaderCarrier,
                                                             ec: ExecutionContext): Future[DesOutcome[CreateBFLossResponse]] = {

    val nino = createBFLossRequest.nino.nino

    def doIt(implicit hc: HeaderCarrier) =
      http.POST[BFLoss, DesOutcome[CreateBFLossResponse]](s"${appConfig.desBaseUrl}/income-tax/brought-forward-losses/$nino",
                                                          createBFLossRequest.broughtForwardLoss)

    doIt(desHeaderCarrier)
  }

  def amendBFLoss(amendBFLossRequest: AmendBFLossRequest)(implicit hc: HeaderCarrier,
                                                          ec: ExecutionContext): Future[DesOutcome[AmendBFLossResponse]] = {

    val nino   = amendBFLossRequest.nino.nino
    val lossId = amendBFLossRequest.lossId

    def doIt(implicit hc: HeaderCarrier) =
      http.PUT[AmendBFLoss, DesOutcome[AmendBFLossResponse]](s"${appConfig.desBaseUrl}/income-tax/brought-forward-losses/$nino/$lossId",
                                                             amendBFLossRequest.amendBroughtForwardLoss)

    doIt(desHeaderCarrier)
  }

  def deleteBFLoss(request: DeleteBFLossRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DesOutcome[Unit]] = {
    val nino   = request.nino.nino
    val lossId = request.lossId

    def doIt(implicit hc: HeaderCarrier) =
      http.DELETE[DesOutcome[Unit]](s"${appConfig.desBaseUrl}/income-tax/brought-forward-losses/$nino/$lossId")

    doIt(desHeaderCarrier)
  }

  def retrieveBFLoss(request: RetrieveBFLossRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DesOutcome[RetrieveBFLossResponse]] = {
    val nino   = request.nino.nino
    val lossId = request.lossId

    def doIt(implicit hc: HeaderCarrier): Future[DesOutcome[RetrieveBFLossResponse]] = {
      http.GET[DesOutcome[RetrieveBFLossResponse]](s"${appConfig.desBaseUrl}/income-tax/brought-forward-losses/$nino/$lossId")
    }

    doIt(desHeaderCarrier)
  }
}
