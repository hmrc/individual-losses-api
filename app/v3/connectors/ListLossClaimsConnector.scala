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
import v3.models.request.listLossClaims.ListLossClaimsRequestData
import v3.models.response.listLossClaims.{ListLossClaimsItem, ListLossClaimsResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ListLossClaimsConnector @Inject() (val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  type ListLossClaimsOutcome = DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]]

  private val DEFAULT_TAX_YEARS = List("2019-20", "2020-21", "2021-22", "2022-23").map(TaxYear.fromMtd)

  private val NOT_FOUND_CODE = "NOT_FOUND"

  def listLossClaims(
      request: ListLossClaimsRequestData)(implicit hc: HeaderCarrier, ec: ExecutionContext, correlationId: String): Future[ListLossClaimsOutcome] = {

    import request._

    val params = List(
      "incomeSourceId"   -> businessId.map(_.businessId),
      "incomeSourceType" -> typeOfLoss.flatMap(_.toIncomeSourceType).map(_.toString),
      "claimType"        -> typeOfClaim.map(_.toReliefClaimed.toString)
    ).collect { case (key, Some(value)) =>
      key -> value
    }

    def send(taxYear: TaxYear): Future[ListLossClaimsOutcome] = get(
      uri =
        TaxYearSpecificIfsUri[ListLossClaimsResponse[ListLossClaimsItem]](s"income-tax/${taxYear.asTysDownstream}/claims-for-relief/${nino.nino}"),
      queryParams = params
    )

    taxYearClaimedFor match {
      case Some(taxYear) => send(taxYear)
      case _             => Future.traverse(DEFAULT_TAX_YEARS)(send).map(combineResponses)
    }
  }

  private def combineResponses(responses: Seq[ListLossClaimsOutcome])(implicit correlationId: String): ListLossClaimsOutcome = {
    lazy val claims: Seq[ListLossClaimsItem] = responses.collect({ case Right(success) => success.responseData.claims }).flatten

    responses.find(isError) match {
      case Some(error)         => error
      case _ if claims.isEmpty => Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(NOT_FOUND_CODE))))
      case _                   => Right(ResponseWrapper(correlationId, ListLossClaimsResponse(claims)))
    }
  }

  private def isError(outcome: ListLossClaimsOutcome): Boolean = {
    outcome match {
      case Right(_)                                          => false
      case Left(ResponseWrapper(_, DownstreamErrors(codes))) => codes.exists(_.code != NOT_FOUND_CODE)
      case Left(_)                                           => true
    }
  }

}
