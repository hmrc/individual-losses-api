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

import api.connectors.{ConnectorSpec, DownstreamOutcome}
import api.endpoints.bfLoss.domain.anyVersion.IncomeSourceType
import api.endpoints.bfLoss.list.v3.request.ListBFLossesRequest
import api.endpoints.bfLoss.list.v3.response.{ListBFLossesItem, ListBFLossesResponse}
import api.fixtures.v3.ListBFLossesFixtures._
import api.models.ResponseWrapper
import api.models.domain.{Nino, TaxYear}
import api.models.errors.{DownstreamErrorCode, DownstreamErrors, InternalError, OutboundError}

import scala.concurrent.Future

class ListBFLossesConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

  trait Test {
    _: ConnectorTest =>

    val connector: ListBFLossesConnector = new ListBFLossesConnector(http = mockHttpClient, appConfig = mockAppConfig)

    protected def success(taxYear: String) = Right(ResponseWrapper(correlationId, singleBFLossesResponseModel(taxYear)))

    protected def downstreamError(code: String) = Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(code))))

    val outboundError = Left(ResponseWrapper(correlationId, OutboundError(InternalError)))

  }

  "listBFLosses" should {
    "a valid request is supplied with no tax year parameter" should {
      "return a successful combined response" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/brought-forward-losses/19-20/$nino") returns Future.successful(success("2019-20"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/20-21/$nino") returns Future.successful(success("2020-21"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/21-22/$nino") returns Future.successful(success("2021-22"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/22-23/$nino") returns Future.successful(success("2022-23"))

        listBFLossesResult(connector) shouldBe Right(ResponseWrapper(correlationId, multipleBFLossesResponseModel))
      }

      "return a successful response ignoring 404 NOT_FOUND responses" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/brought-forward-losses/19-20/$nino") returns Future.successful(success("2019-20"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/20-21/$nino") returns Future.successful(success("2020-21"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/21-22/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/22-23/$nino") returns Future.successful(success("2022-23"))

        listBFLossesResult(connector) shouldBe Right(
          ResponseWrapper(
            correlationId,
            ListBFLossesResponse(
              List(
                listBFLossesModel("2019-20"),
                listBFLossesModel("2020-21"),
                listBFLossesModel("2022-23")
              )
            )
          )
        )
      }

      "return a 404 NOT_FOUND response if all responses are NOT_FOUND" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/brought-forward-losses/19-20/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/20-21/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/21-22/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/22-23/$nino") returns Future.successful(downstreamError("NOT_FOUND"))

        listBFLossesResult(connector) shouldBe Left(
          ResponseWrapper(
            correlationId,
            DownstreamErrors.single(DownstreamErrorCode("NOT_FOUND"))
          )
        )
      }

      "return the error response if any request errors" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/brought-forward-losses/19-20/$nino") returns Future.successful(success("2019-20"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/20-21/$nino") returns Future.successful(success("2020-21"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/21-22/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/22-23/$nino") returns Future.successful(downstreamError("INVALID_TAXABLE_ENTITY_ID"))

        listBFLossesResult(connector) shouldBe Left(
          ResponseWrapper(
            correlationId,
            DownstreamErrors.single(DownstreamErrorCode("INVALID_TAXABLE_ENTITY_ID"))
          )
        )
      }

      "return the error response for an OutboundError" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/brought-forward-losses/19-20/$nino") returns Future.successful(success("2019-20"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/20-21/$nino") returns Future.successful(success("2020-21"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/21-22/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/brought-forward-losses/22-23/$nino") returns Future.successful(outboundError)

        listBFLossesResult(connector) shouldBe Left(
          ResponseWrapper(correlationId, OutboundError(InternalError))
        )
      }

    }

    "a valid request is supplied with only the tax year parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/brought-forward-losses/23-24/$nino") returns Future.successful(success("2023-24"))
        listBFLossesResult(connector, Some(TaxYear.fromMtd("2023-24"))) shouldBe success("2023-24")
      }
    }

    "provided with a income source id parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(url = s"$baseUrl/income-tax/brought-forward-losses/23-24/$nino", queryParams = List(("incomeSourceId", "testId"))) returns Future
          .successful(success("2023-24"))

        listBFLossesResult(connector = connector, Some(TaxYear.fromMtd("2023-24")), businessId = Some("testId")) shouldBe success("2023-24")
      }
    }

    "provided with a income source type parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/23-24/$nino",
          queryParams = List(("incomeSourceType", "02"))
        ) returns Future.successful(success("2023-24"))

        listBFLossesResult(
          connector = connector,
          taxYear = Some(TaxYear.fromMtd("2023-24")),
          incomeSourceType = Some(IncomeSourceType.`02`)) shouldBe success("2023-24")
      }
    }

    "provided with all parameters" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/23-24/$nino",
          queryParams = List(("incomeSourceId", "testId"), ("incomeSourceType", "02"))
        ) returns Future.successful(success("2023-24"))

        listBFLossesResult(
          connector = connector,
          taxYear = Some(TaxYear.fromMtd("2023-24")),
          businessId = Some("testId"),
          incomeSourceType = Some(IncomeSourceType.`02`)
        ) shouldBe success("2023-24")
      }
    }

    def listBFLossesResult(connector: ListBFLossesConnector,
                           taxYear: Option[TaxYear] = None,
                           incomeSourceType: Option[IncomeSourceType] = None,
                           businessId: Option[String] = None): DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]] =
      await(
        connector.listBFLosses(
          ListBFLossesRequest(
            nino = Nino(nino),
            taxYearBroughtForwardFrom = taxYear,
            incomeSourceType = incomeSourceType,
            businessId = businessId
          )))

  }

}
