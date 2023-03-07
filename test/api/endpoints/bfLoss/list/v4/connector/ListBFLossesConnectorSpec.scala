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

package api.endpoints.bfLoss.list.v4.connector

import api.connectors.{ ConnectorSpec, DownstreamOutcome }
import api.endpoints.bfLoss.domain.anyVersion.{ IncomeSourceType, TypeOfLoss }
import api.endpoints.bfLoss.list.v4.request.ListBFLossesRequest
import api.endpoints.bfLoss.list.v4.response.{ ListBFLossesItem, ListBFLossesResponse }
import api.models.ResponseWrapper
import api.models.domain.{ Nino, TaxYear }

import scala.concurrent.Future

class ListBFLossesConnectorSpec extends ConnectorSpec {

  private val nino    = "AA123456A"
  private val taxYear = TaxYear.fromMtd("2023-24")

  def makeRequest(taxYear: TaxYear, incomeSourceType: Option[IncomeSourceType] = None, businessId: Option[String] = None): ListBFLossesRequest =
    ListBFLossesRequest(
      nino = Nino(nino),
      taxYearBroughtForwardFrom = taxYear,
      incomeSourceType = incomeSourceType,
      businessId = businessId
    )

  def makeResponse(taxYear: TaxYear = TaxYear("2019")): ListBFLossesResponse[ListBFLossesItem] =
    ListBFLossesResponse(
      List(ListBFLossesItem("lossId", "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, s"${taxYear.asMtd}", "lastModified"))
    )

  trait Test {
    _: ConnectorTest =>

    val connector: ListBFLossesConnector = new ListBFLossesConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

  "listBFLosses()" should {

    "return the expected response" when {
      "downstream returns OK" when {
        "the connector sends a request with just the tax year parameter" in new TysIfsTest with Test {
          val responseData: ListBFLossesResponse[ListBFLossesItem] = makeResponse(taxYear = taxYear)
          val request: ListBFLossesRequest                         = makeRequest(taxYear = taxYear)

          val downstreamResponse = Right(ResponseWrapper(correlationId, responseData))

          willGet(
            url = s"$baseUrl/income-tax/brought-forward-losses/${taxYear.asTysDownstream}/$nino"
          ).returns(Future.successful(downstreamResponse))

          val result: DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]] = await(connector.listBFLosses(request))
          result shouldBe downstreamResponse
        }

        "a valid request with all parameters" in new TysIfsTest with Test {
          val responseData: ListBFLossesResponse[ListBFLossesItem] = makeResponse(taxYear)
          val request: ListBFLossesRequest =
            makeRequest(taxYear = taxYear, businessId = Some("testId"), incomeSourceType = Some(IncomeSourceType.`01`))

          val downstreamResponse = Right(ResponseWrapper(correlationId, responseData))

          val queryParams = List(
            ("incomeSourceId", "testId"),
            ("incomeSourceType", "01")
          )

          willGet(
            url = s"$baseUrl/income-tax/brought-forward-losses/${taxYear.asTysDownstream}/$nino",
            queryParams = queryParams
          ).returns(Future.successful(downstreamResponse))

          val result: DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]] = await(connector.listBFLosses(request))
          result shouldBe downstreamResponse
        }
      }
    }

  }

}
