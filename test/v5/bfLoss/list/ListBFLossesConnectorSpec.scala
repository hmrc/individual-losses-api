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

package v5.bfLoss.list

import api.connectors.{ConnectorSpec, DownstreamOutcome}
import api.models.domain.{BusinessId, Nino, TaxYear}
import api.models.outcomes.ResponseWrapper
import v5.bfLosses.list.ListBFLossesConnector
import v5.bfLosses.list.def1.model.request.Def1_ListBFLossesRequestData
import v5.bfLosses.list.def1.model.response.{ListBFLossesItem, Def1_ListBFLossesResponse}
import v5.bfLosses.list.model._
import v5.bfLosses.list.model.request.ListBFLossesRequestData
import v5.bfLosses.list.model.response.ListBFLossesResponse

import scala.concurrent.Future

class ListBFLossesConnectorSpec extends ConnectorSpec {

  private val nino    = "AA123456A"
  private val taxYear = TaxYear.fromMtd("2023-24")

  def makeRequest(taxYear: TaxYear,
                  incomeSourceType: Option[IncomeSourceType] = None,
                  businessId: Option[BusinessId] = None): ListBFLossesRequestData =
    Def1_ListBFLossesRequestData(
      nino = Nino(nino),
      taxYearBroughtForwardFrom = taxYear,
      incomeSourceType = incomeSourceType,
      businessId = businessId
    )

  def makeResponse(taxYear: TaxYear = TaxYear("2019")): Def1_ListBFLossesResponse =
    Def1_ListBFLossesResponse(
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
          val responseData: ListBFLossesResponse = makeResponse(taxYear = taxYear)
          val request: ListBFLossesRequestData                     = makeRequest(taxYear = taxYear)

          val downstreamResponse = Right(ResponseWrapper(correlationId, responseData))

          willGet(
            url = s"$baseUrl/income-tax/brought-forward-losses/${taxYear.asTysDownstream}/$nino"
          ).returns(Future.successful(downstreamResponse))

          val result: DownstreamOutcome[ListBFLossesResponse] = await(connector.listBFLosses(request))
          result shouldBe downstreamResponse
        }

        "a valid request with all parameters" in new TysIfsTest with Test {
          val responseData: ListBFLossesResponse = makeResponse(taxYear)
          val request: ListBFLossesRequestData =
            makeRequest(taxYear = taxYear, businessId = Some(BusinessId("testId")), incomeSourceType = Some(IncomeSourceType.`01`))

          val downstreamResponse = Right(ResponseWrapper(correlationId, responseData))

          val queryParams = List(
            ("incomeSourceId", "testId"),
            ("incomeSourceType", "01")
          )

          willGet(
            url = s"$baseUrl/income-tax/brought-forward-losses/${taxYear.asTysDownstream}/$nino",
            parameters = queryParams
          ).returns(Future.successful(downstreamResponse))

          val result: DownstreamOutcome[ListBFLossesResponse] = await(connector.listBFLosses(request))
          result shouldBe downstreamResponse
        }
      }
    }

  }

}
