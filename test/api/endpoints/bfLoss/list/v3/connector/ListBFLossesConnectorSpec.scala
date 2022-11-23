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

package api.endpoints.bfLoss.list.v3.connector

import api.connectors.ConnectorSpec
import api.endpoints.bfLoss.connector.v3.BFLossConnector
import api.endpoints.bfLoss.domain.v3.{ IncomeSourceType, TypeOfLoss }
import api.endpoints.bfLoss.list.v3.request.ListBFLossesRequest
import api.endpoints.bfLoss.list.v3.response.{ ListBFLossesItem, ListBFLossesResponse }
import api.models.errors.{ LossIdFormatError, MultipleErrors, NinoFormatError, SingleError }
import api.models.ResponseWrapper
import api.models.domain.{ Nino, TaxYear }

import scala.concurrent.Future

class ListBFLossesConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

  def makeRequest(taxYear: Option[TaxYear] = None,
                  incomeSourceType: Option[IncomeSourceType] = None,
                  businessId: Option[String] = None): ListBFLossesRequest =
    ListBFLossesRequest(
      nino = Nino(nino),
      taxYearBroughtForwardFrom = taxYear,
      incomeSourceType = incomeSourceType,
      businessId = businessId
    )

  trait Test {
    _: ConnectorTest =>

    val connector: BFLossConnector = new BFLossConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

  "listBFLosses" when {
    "return a successful response with the correct correlationId" should {
      val response =
        ListBFLossesResponse(Seq(ListBFLossesItem("lossId", "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, "2019-20", "lastModified")))

      "a valid non-TYS request with no provided parameters is supplied" in new IfsTest with Test {
        val expected = Right(ResponseWrapper(correlationId, response))

        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
        ).returns(Future.successful(expected))

        val request: ListBFLossesRequest = makeRequest()

        await(connector.listBFLosses(request)) shouldBe expected
      }

      "a valid non-TYS request with a tax year parameter provided is supplied" in new IfsTest with Test {
        def taxYear: TaxYear = TaxYear.fromMtd("2018-19")
        val expected         = Right(ResponseWrapper(correlationId, response))

        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
          parameters = "taxYear" -> taxYear.asDownstream
        ).returns(Future.successful(expected))

        val request: ListBFLossesRequest = makeRequest(taxYear = Some(taxYear))

        await(connector.listBFLosses(request)) shouldBe expected
      }

      "a valid non-TYS request with a income source id parameter provided is supplied" in new IfsTest with Test {
        val expected = Right(ResponseWrapper(correlationId, response))

        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
          parameters = "incomeSourceId" -> "testId"
        ).returns(Future.successful(expected))

        val request: ListBFLossesRequest = makeRequest(businessId = Some("testId"))

        await(connector.listBFLosses(request)) shouldBe expected
      }

      "a valid non-TYS request with a income source type parameter provided is supplied" in new IfsTest with Test {
        val expected = Right(ResponseWrapper(correlationId, response))

        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
          parameters = "incomeSourceType" -> "02"
        ).returns(Future.successful(expected))

        val request: ListBFLossesRequest = makeRequest(incomeSourceType = Some(IncomeSourceType.`02`))

        await(connector.listBFLosses(request)) shouldBe expected
      }

      "a valid non-TYS request with all parameters provided is supplied" in new IfsTest with Test {
        def taxYear: TaxYear = TaxYear.fromMtd("2018-19")
        val expected         = Right(ResponseWrapper(correlationId, response))

        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
          parameters = "taxYear" -> taxYear.asDownstream,
          "incomeSourceId"   -> "testId",
          "incomeSourceType" -> "01"
        ).returns(Future.successful(expected))

        val request: ListBFLossesRequest =
          makeRequest(taxYear = Some(taxYear), businessId = Some("testId"), incomeSourceType = Some(IncomeSourceType.`01`))

        await(connector.listBFLosses(request)) shouldBe expected
      }
    }

    "return an unsuccessful response" should {
      "a non-valid request with a single error be supplied" in new IfsTest with Test {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
        ).returns(Future.successful(expected))

        val request: ListBFLossesRequest = makeRequest()

        await(connector.listBFLosses(request)) shouldBe expected
      }

      "a non-valid request with multiple errors be supplied" in new IfsTest with Test {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, LossIdFormatError))))

        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
        ).returns(Future.successful(expected))

        val request: ListBFLossesRequest = makeRequest()

        await(connector.listBFLosses(request)) shouldBe expected
      }
    }
  }
}
