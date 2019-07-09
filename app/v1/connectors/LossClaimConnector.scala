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
import v1.models.des.{AmendBFLossResponse, CreateBFLossResponse, ListBFLossesResponse, RetrieveBFLossResponse}
import v1.models.domain.{AmendBFLoss, BFLoss}
import v1.models.requestData._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LossClaimConnector @Inject()(http: HttpClient, appConfig: AppConfig) extends DesConnector {

  def createLossClaim(createBFLossRequest: CreateLossClaimRequest)(implicit hc: HeaderCarrier,
                                                             ec: ExecutionContext): Future[DesOutcome[CreateBFLossResponse]] = {

    val nino = createBFLossRequest.nino.nino

    def doIt(implicit hc: HeaderCarrier) =
      http.POST[BFLoss, DesOutcome[CreateBFLossResponse]](s"${appConfig.desBaseUrl}/income-tax/brought-forward-losses/$nino",
                                                          createBFLossRequest.broughtForwardLoss)

    doIt(desHeaderCarrier(appConfig))
  }
}
