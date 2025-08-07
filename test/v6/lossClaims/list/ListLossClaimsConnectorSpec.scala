/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.list

import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.errors.{DownstreamErrorCode, DownstreamErrors, InternalError, OutboundError}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v6.lossClaims.common.models.{TypeOfClaim, TypeOfLoss}
import v6.lossClaims.fixtures.ListLossClaimsFixtures.*
import v6.lossClaims.list.def1.request.Def1_ListLossClaimsRequestData
import v6.lossClaims.list.model.response.ListLossClaimsResponse

import scala.concurrent.Future

class ListLossClaimsConnectorSpec extends ConnectorSpec {

  private val nino = Nino("AA123456A")

  "list LossClaims" when {
    "a valid request is supplied with only the tax year parameter" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        willGet(url"$baseUrl/income-tax/23-24/claims-for-relief/$nino")
          .returning(Future.successful(success("2023-24")))

        val result: DownstreamOutcome[ListLossClaimsResponse] =
          listLossClaimsResult(connector, TaxYear.fromMtd("2023-24"))

        result shouldBe success("2023-24")
      }
    }

    "provided with a income source id parameter" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        willGet(url = url"$baseUrl/income-tax/23-24/claims-for-relief/$nino", List(("incomeSourceId", "testId")))
          .returning(Future.successful(success("2023-24")))

        val result: DownstreamOutcome[ListLossClaimsResponse] =
          listLossClaimsResult(connector = connector, TaxYear.fromMtd("2023-24"), businessId = Some(BusinessId("testId")))

        result shouldBe success("2023-24")
      }
    }

    "provided with a income source type parameter" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        willGet(
          url = url"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          parameters = List(("incomeSourceType", "02"))
        ).returning(Future.successful(success("2023-24")))

        val result: DownstreamOutcome[ListLossClaimsResponse] =
          listLossClaimsResult(connector = connector, TaxYear.fromMtd("2023-24"), typeOfLoss = Some(TypeOfLoss.`uk-property`))

        result shouldBe success("2023-24")
      }
    }

    "provided with a claim type parameter" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        willGet(
          url = url"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          parameters = List(("claimType", "CSGI"))
        ).returning(Future.successful(success("2023-24")))

        val result: DownstreamOutcome[ListLossClaimsResponse] =
          listLossClaimsResult(connector = connector, TaxYear.fromMtd("2023-24"), claimType = Some(TypeOfClaim.`carry-sideways`))

        result shouldBe success("2023-24")
      }
    }

    "provided with all parameters" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        willGet(
          url = url"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          parameters = List(("incomeSourceId", "testId"), ("incomeSourceType", "01"), ("claimType", "CSGI"))
        ).returning(Future.successful(success("2023-24")))

        val result: DownstreamOutcome[ListLossClaimsResponse] = listLossClaimsResult(
          connector = connector,
          taxYear = TaxYear.fromMtd("2023-24"),
          businessId = Some(BusinessId("testId")),
          typeOfLoss = Some(TypeOfLoss.`self-employment`),
          claimType = Some(TypeOfClaim.`carry-sideways`)
        )

        result shouldBe success("2023-24")
      }
    }

    def listLossClaimsResult(connector: ListLossClaimsConnector,
                             taxYear: TaxYear,
                             typeOfLoss: Option[TypeOfLoss] = None,
                             businessId: Option[BusinessId] = None,
                             claimType: Option[TypeOfClaim] = None): DownstreamOutcome[ListLossClaimsResponse] =
      await(
        connector.listLossClaims(
          Def1_ListLossClaimsRequestData(
            nino = nino,
            taxYearClaimedFor = taxYear,
            typeOfLoss = typeOfLoss,
            businessId = businessId,
            typeOfClaim = claimType
          )))
  }

  trait Test { self: ConnectorTest =>

    protected val connector: ListLossClaimsConnector =
      new ListLossClaimsConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)

    protected def success(taxYear: String): Either[ResponseWrapper[DownstreamErrors], ResponseWrapper[ListLossClaimsResponse]] =
      Right(ResponseWrapper(correlationId, singleClaimResponseModel(taxYear)))

    protected def downstreamError(code: String): Either[ResponseWrapper[DownstreamErrors], ResponseWrapper[ListLossClaimsResponse]] =
      Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(code))))

    protected val outboundError: Either[ResponseWrapper[OutboundError], ResponseWrapper[ListLossClaimsResponse]] =
      Left(ResponseWrapper(correlationId, OutboundError(InternalError)))

  }

}
