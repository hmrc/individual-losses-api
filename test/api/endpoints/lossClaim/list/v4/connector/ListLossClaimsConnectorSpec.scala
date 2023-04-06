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

package api.endpoints.lossClaim.list.v4.connector

import api.connectors.{ ConnectorSpec, DownstreamOutcome }
import api.endpoints.lossClaim.domain.v3.{ TypeOfClaim, TypeOfLoss }
import api.endpoints.lossClaim.list.v4.request.ListLossClaimsRequest
import api.endpoints.lossClaim.list.v4.response.{ ListLossClaimsItem, ListLossClaimsResponse }
import api.fixtures.v4.ListLossClaimsFixtures._
import api.models.ResponseWrapper
import api.models.domain.{ Nino, TaxYear }
import api.models.errors.{ DownstreamErrorCode, DownstreamErrors, InternalError, OutboundError }

import scala.concurrent.Future

class ListLossClaimsConnectorSpec extends ConnectorSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890ag"

  trait Test {
    _: ConnectorTest =>
    val connector: ListLossClaimsConnector = new ListLossClaimsConnector(http = mockHttpClient, appConfig = mockAppConfig)

    protected def success(taxYear: String) = Right(ResponseWrapper(correlationId, singleClaimResponseModel(taxYear)))

    protected def downstreamError(code: String) = Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(code))))

    val outboundError = Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
  }

  "list LossClaims" when {
    "a valid request is supplied with only the tax year parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/23-24/claims-for-relief/$nino") returns Future.successful(success("2023-24"))
        listLossClaimsResult(connector, TaxYear.fromMtd("2023-24")) shouldBe success("2023-24")
      }
    }

    "provided with a income source id parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino", queryParams = List(("incomeSourceId", "testId"))) returns Future
          .successful(success("2023-24"))

        listLossClaimsResult(connector = connector, TaxYear.fromMtd("2023-24"), businessId = Some("testId")) shouldBe success("2023-24")
      }
    }

    "provided with a income source type parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(
          url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          queryParams = List(("incomeSourceType", "02"))
        ) returns Future.successful(success("2023-24"))

        listLossClaimsResult(connector = connector, TaxYear.fromMtd("2023-24"), typeOfLoss = Some(TypeOfLoss.`uk-property-non-fhl`)) shouldBe success(
          "2023-24")
      }
    }

    "provided with a claim type parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(
          url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          queryParams = List(("claimType", "CSGI"))
        ) returns Future.successful(success("2023-24"))

        listLossClaimsResult(connector = connector, TaxYear.fromMtd("2023-24"), claimType = Some(TypeOfClaim.`carry-sideways`)) shouldBe success(
          "2023-24")
      }
    }

    "provided with all parameters" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(
          url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          queryParams = List(("incomeSourceId", "testId"), ("incomeSourceType", "01"), ("claimType", "CSGI"))
        ) returns Future.successful(success("2023-24"))

        listLossClaimsResult(
          connector = connector,
          taxYear = TaxYear("2024"),
          businessId = Some("testId"),
          typeOfLoss = Some(TypeOfLoss.`self-employment`),
          claimType = Some(TypeOfClaim.`carry-sideways`)
        ) shouldBe success("2023-24")
      }
    }

    def listLossClaimsResult(connector: ListLossClaimsConnector,
                             taxYear: TaxYear,
                             typeOfLoss: Option[TypeOfLoss] = None,
                             businessId: Option[String] = None,
                             claimType: Option[TypeOfClaim] = None): DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]] =
      await(
        connector.listLossClaims(
          ListLossClaimsRequest(
            nino = Nino(nino),
            taxYearClaimedFor = taxYear,
            typeOfLoss = typeOfLoss,
            businessId = businessId,
            typeOfClaim = claimType
          )))
  }

}
