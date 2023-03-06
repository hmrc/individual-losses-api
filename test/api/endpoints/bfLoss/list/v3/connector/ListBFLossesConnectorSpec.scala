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

package api.endpoints.bfLoss.list.v3.connector

import api.connectors.{ ConnectorSpec, DownstreamOutcome }
import api.endpoints.bfLoss.connector.v3.BFLossConnector
import api.endpoints.bfLoss.domain.anyVersion.{ IncomeSourceType, TypeOfLoss }
import api.endpoints.bfLoss.list.v3.request.ListBFLossesRequest
import api.endpoints.bfLoss.list.v3.response.{ ListBFLossesItem, ListBFLossesResponse }
import api.models.ResponseWrapper
import api.models.domain.{ Nino, TaxYear }

import scala.concurrent.Future

class ListBFLossesConnectorSpec extends ConnectorSpec {

  private val nino = "AA123456A"

  "listBFLosses" when {

    "a valid request is made with no taxYear parameter" should {
      "return OK" in new Test {
        private val request = makeRequest(incomeSourceType = Some(IncomeSourceType.`02`))

        private val queryParams = List(
          ("incomeSourceType", "02")
        )

        willGet(s"$baseUrl/income-tax/brought-forward-losses/19-20/$nino", queryParams).returns(Future.successful(success("2019-20")))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/20-21/$nino", queryParams).returns(Future.successful(success("2020-21")))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/21-22/$nino", queryParams).returns(Future.successful(success("2021-22")))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/22-23/$nino", queryParams).returns(Future.successful(success("2022-23")))

        val result: DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]] = await(connector.listBFLosses(request))
        result shouldBe Right(ResponseWrapper(correlationId, bfLosses("2019-20", "2020-21", "2021-22", "2022-23")))
      }
    }

    "a valid request is made with all parameters" in new Test {
      def taxYear: TaxYear = TaxYear.fromMtd("2018-19")

      val request: ListBFLossesRequest =
        makeRequest(taxYear = Some(taxYear), businessId = Some("testId"), incomeSourceType = Some(IncomeSourceType.`01`))
      val response: ListBFLossesResponse[ListBFLossesItem] = singleClaimResponseModel(taxYear.asMtd)

      val expected = Right(ResponseWrapper(correlationId, response))

      val queryParams: Seq[(String, String)] = List(
        ("incomeSourceId", "testId"),
        ("incomeSourceType", "01")
      )

      willGet(
        url = s"$baseUrl/income-tax/brought-forward-losses/18-19/$nino",
        queryParams = queryParams
      ).returns(Future.successful(expected))

      val result: DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]] = await(connector.listBFLosses(request))
      result shouldBe expected
    }
  }

  trait Test extends TysIfsTest {
    _: ConnectorTest =>

    val connector: BFLossConnector = new BFLossConnector(http = mockHttpClient, appConfig = mockAppConfig)

    protected def success(taxYear: String) =
      Right(ResponseWrapper(correlationId, singleClaimResponseModel(taxYear)))

    protected def singleClaimResponseModel(taxYear: String): ListBFLossesResponse[ListBFLossesItem] =
      ListBFLossesResponse(
        List(bfLoss(TaxYear.fromMtd(taxYear)))
      )

  }

  private def makeRequest(taxYear: Option[TaxYear] = None,
                          incomeSourceType: Option[IncomeSourceType],
                          businessId: Option[String] = None): ListBFLossesRequest =
    ListBFLossesRequest(
      nino = Nino(nino),
      taxYearBroughtForwardFrom = taxYear,
      incomeSourceType = incomeSourceType,
      businessId = businessId
    )

  private def bfLoss(taxYear: TaxYear): ListBFLossesItem =
    ListBFLossesItem("lossId", "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, s"${taxYear.asMtd}", "lastModified")

  private def bfLosses(taxYears: String*): ListBFLossesResponse[ListBFLossesItem] =
    ListBFLossesResponse(
      taxYears
        .map(taxYear =>
          ListBFLossesItem("lossId", "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, s"${TaxYear.fromMtd(taxYear).asMtd}", "lastModified"))
        .toList
    )

}
