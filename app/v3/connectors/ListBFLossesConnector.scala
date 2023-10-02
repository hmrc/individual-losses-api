/*
 * Copyright 2023 HM Revenue & Customs
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

import api.connectors.DownstreamUri.TaxYearSpecificIfsUri
import api.connectors.httpparsers.StandardDownstreamHttpParser._
import api.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import api.models.ResponseWrapper
import api.models.domain.TaxYear
import api.models.errors.{DownstreamErrorCode, DownstreamErrors}
import config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v3.models.request.listBFLosses.ListBFLossesRequestData
import v3.models.response.listBFLosses.{ListBFLossesItem, ListBFLossesResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ListBFLossesConnector @Inject() (val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  type ListBFLossesOutcome = DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]]

  private val DEFAULT_TAX_YEARS: Seq[TaxYear] = List("2019-20", "2020-21", "2021-22", "2022-23").map(TaxYear.fromMtd)

  private val NOT_FOUND_CODE = "NOT_FOUND"

  def listBFLosses(
      request: ListBFLossesRequestData)(implicit hc: HeaderCarrier, ec: ExecutionContext, correlationId: String): Future[ListBFLossesOutcome] = {
    import request._

    val queryParams = List(
      businessId.map("incomeSourceId"         -> _.toString),
      incomeSourceType.map("incomeSourceType" -> _.toString)
    ).flatten

    def send(taxYear: TaxYear): Future[ListBFLossesOutcome] = get(
      uri = TaxYearSpecificIfsUri[ListBFLossesResponse[ListBFLossesItem]](s"income-tax/brought-forward-losses/${taxYear.asTysDownstream}/$nino"),
      queryParams = queryParams
    )

    taxYearBroughtForwardFrom match {
      case Some(taxYear) => send(taxYear)
      case _             => Future.traverse(DEFAULT_TAX_YEARS)(send).map(combineResponses)
    }
  }

  private def combineResponses(responses: Seq[ListBFLossesOutcome])(implicit correlationId: String): ListBFLossesOutcome = {
    lazy val losses = responses.collect({ case Right(success) => success.responseData.losses }).flatten

    responses.find(isError) match {
      case Some(error)         => error
      case _ if losses.isEmpty => Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(NOT_FOUND_CODE))))
      case _                   => Right(ResponseWrapper(correlationId, ListBFLossesResponse(losses)))
    }
  }

  private def isError(outcome: ListBFLossesOutcome): Boolean = {
    outcome match {
      case Right(_)                                          => false
      case Left(ResponseWrapper(_, DownstreamErrors(codes))) => codes.exists(_.code != NOT_FOUND_CODE)
      case Left(_)                                           => true
    }
  }

}
