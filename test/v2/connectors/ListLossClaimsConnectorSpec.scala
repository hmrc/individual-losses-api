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

package v2.connectors

import api.connectors.DownstreamOutcome
import api.connectors.v2.LossClaimConnector
import api.models.domain.Nino
import api.models.domain.v2.TypeOfClaim
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import v2.models.des._
import v2.models.requestData._

import scala.concurrent.Future

class ListLossClaimsConnectorSpec extends LossClaimConnectorSpec {

  "list LossClaims" when {

    val claimId2: String = "AAZZ1234567890b"

    "a valid request is supplied with no query parameters" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(
              Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`), LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))
          ))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector) shouldBe expected
      }
    }

    "provided with a tax year parameter" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Left(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(
              Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`), LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))
          ))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("taxYear", "2019")),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector = connector, taxYear = Some(DesTaxYear("2019"))) shouldBe expected
      }
    }

    "provided with a income source id parameter" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Left(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(
              Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`), LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))
          ))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("incomeSourceId", "testId")),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector = connector, businessId = Some("testId")) shouldBe expected
      }
    }

    "provided with a income source type parameter" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Left(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(
              Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`), LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))
          ))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("incomeSourceType", "02")),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector = connector, incomeSourceType = Some(IncomeSourceType.`02`)) shouldBe expected
      }
    }

    "provided with a claim type parameter" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Left(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(
              Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`), LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))
          ))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("claimType", "carry-sideways")),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector = connector, claimType = Some(TypeOfClaim.`carry-sideways`)) shouldBe expected
      }
    }

    "provided with all parameters" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Left(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(
              Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`), LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))
          ))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("taxYear", "2019"), ("incomeSourceId", "testId"), ("incomeSourceType", "01"), ("claimType", "carry-sideways")),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(
          connector = connector,
          taxYear = Some(DesTaxYear("2019")),
          businessId = Some("testId"),
          incomeSourceType = Some(IncomeSourceType.`01`),
          claimType = Some(TypeOfClaim.`carry-sideways`)
        ) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("taxYear", "2019")),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector = connector, Some(DesTaxYear("2019"))) shouldBe expected
      }
    }

    def listLossClaimsResult(connector: LossClaimConnector,
                             taxYear: Option[DesTaxYear] = None,
                             incomeSourceType: Option[IncomeSourceType] = None,
                             businessId: Option[String] = None,
                             claimType: Option[TypeOfClaim] = None): DownstreamOutcome[ListLossClaimsResponse[LossClaimId]] =
      await(
        connector.listLossClaims(
          ListLossClaimsRequest(
            nino = Nino(nino),
            taxYear = taxYear,
            incomeSourceType = incomeSourceType,
            businessId = businessId,
            claimType = claimType
          )))
  }
}
