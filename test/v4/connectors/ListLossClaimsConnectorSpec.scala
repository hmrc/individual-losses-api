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

import api.connectors.{ConnectorSpec, DownstreamOutcome}
import api.models.domain.{BusinessId, Nino, TaxYear}
import api.models.errors.{DownstreamErrorCode, DownstreamErrors, InternalError, OutboundError}
import api.models.outcomes.ResponseWrapper
import v4.fixtures.ListLossClaimsFixtures._
import v4.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}
import v4.models.request.listLossClaims.ListLossClaimsRequestData
import v4.models.response.listLossClaims.{ListLossClaimsItem, ListLossClaimsResponse}

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

        val result: DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]] =
          listLossClaimsResult(connector, TaxYear.fromMtd("2023-24"))

        result shouldBe success("2023-24")
      }
    }

    "provided with a income source id parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino", parameters = List(("incomeSourceId", "testId"))) returns Future
          .successful(success("2023-24"))

        val result: DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]] =
          listLossClaimsResult(connector = connector, TaxYear.fromMtd("2023-24"), businessId = Some(BusinessId("testId")))

        result shouldBe success("2023-24")
      }
    }

    "provided with a income source type parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(
          url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          parameters = List(("incomeSourceType", "02"))
        ) returns Future.successful(success("2023-24"))

        val result: DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]] =
          listLossClaimsResult(connector = connector, TaxYear.fromMtd("2023-24"), typeOfLoss = Some(TypeOfLoss.`uk-property-non-fhl`))

        result shouldBe success("2023-24")
      }
    }

    "provided with a claim type parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(
          url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          parameters = List(("claimType", "CSGI"))
        ) returns Future.successful(success("2023-24"))

        val result: DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]] =
          listLossClaimsResult(connector = connector, TaxYear.fromMtd("2023-24"), claimType = Some(TypeOfClaim.`carry-sideways`))

        result shouldBe success("2023-24")
      }
    }

    "provided with all parameters" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(
          url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          parameters = List(("incomeSourceId", "testId"), ("incomeSourceType", "01"), ("claimType", "CSGI"))
        ) returns Future.successful(success("2023-24"))

        listLossClaimsResult(
          connector = connector,
          taxYear = TaxYear("2024"),
          businessId = Some(BusinessId("testId")),
          typeOfLoss = Some(TypeOfLoss.`self-employment`),
          claimType = Some(TypeOfClaim.`carry-sideways`)
        ) shouldBe success("2023-24")
      }
    }

    def listLossClaimsResult(connector: ListLossClaimsConnector,
                             taxYear: TaxYear,
                             typeOfLoss: Option[TypeOfLoss] = None,
                             businessId: Option[BusinessId] = None,
                             claimType: Option[TypeOfClaim] = None): DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]] =
      await(
        connector.listLossClaims(
          ListLossClaimsRequestData(
            nino = Nino(nino),
            taxYearClaimedFor = taxYear,
            typeOfLoss = typeOfLoss,
            businessId = businessId,
            typeOfClaim = claimType
          )))
  }

}
