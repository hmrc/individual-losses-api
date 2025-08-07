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

package v4.connectors

import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v4.models.domain.bfLoss.{IncomeSourceType, TypeOfLoss}
import v4.models.request.listLossClaims.ListBFLossesRequestData
import v4.models.response.listBFLosses.{ListBFLossesItem, ListBFLossesResponse}

import scala.concurrent.Future

class ListBFLossesConnectorSpec extends ConnectorSpec {

  private val nino    = "AA123456A"
  private val taxYear = TaxYear.fromMtd("2023-24")

  def makeRequest(taxYear: TaxYear,
                  incomeSourceType: Option[IncomeSourceType] = None,
                  businessId: Option[BusinessId] = None): ListBFLossesRequestData =
    ListBFLossesRequestData(
      nino = Nino(nino),
      taxYearBroughtForwardFrom = taxYear,
      incomeSourceType = incomeSourceType,
      businessId = businessId
    )

  def makeResponse(taxYear: TaxYear = TaxYear.fromMtd("2018-19")): ListBFLossesResponse[ListBFLossesItem] =
    ListBFLossesResponse(
      List(ListBFLossesItem("lossId", "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, s"${taxYear.asMtd}", "lastModified"))
    )

  trait Test {
    self: ConnectorTest =>

    val connector: ListBFLossesConnector = new ListBFLossesConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)

  }

  "listBFLosses()" should {

    "return the expected response" when {
      "downstream returns OK" when {
        "the connector sends a request with just the tax year parameter" in new IfsTest with Test {
          private val responseData       = makeResponse(taxYear = taxYear)
          private val request            = makeRequest(taxYear = taxYear)
          private val downstreamResponse = Right(ResponseWrapper(correlationId, responseData))

          willGet(
            url = url"$baseUrl/income-tax/brought-forward-losses/${taxYear.asTysDownstream}/$nino"
          ).returns(Future.successful(downstreamResponse))

          val result: DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]] = await(connector.listBFLosses(request))
          result shouldBe downstreamResponse
        }

        "a valid request with all parameters" in new IfsTest with Test {
          private val responseData = makeResponse(taxYear)
          private val request = makeRequest(
            taxYear = taxYear,
            businessId = Some(BusinessId("testId")),
            incomeSourceType = Some(IncomeSourceType.`01`)
          )
          private val downstreamResponse = Right(ResponseWrapper(correlationId, responseData))
          private val queryParams = List(
            ("incomeSourceId", "testId"),
            ("incomeSourceType", "01")
          )

          willGet(
            url = url"$baseUrl/income-tax/brought-forward-losses/${taxYear.asTysDownstream}/$nino",
            parameters = queryParams
          ).returning(Future.successful(downstreamResponse))

          val result: DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]] = await(connector.listBFLosses(request))
          result shouldBe downstreamResponse
        }
      }
    }

  }

}
