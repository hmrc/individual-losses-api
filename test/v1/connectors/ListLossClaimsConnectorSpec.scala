/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.connectors

import uk.gov.hmrc.domain.Nino
import v1.models.des._

import v1.models.domain.TypeOfClaim
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData._

import scala.concurrent.Future

class ListLossClaimsConnectorSpec extends LossClaimConnectorSpec {

  "list LossClaims" when {

    val claimId2 = "AAZZ1234567890b"

    "a valid request is supplied with no query parameters" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(DesResponse(correlationId, ListLossClaimsResponse(Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`),
                                                                                   LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/claims-for-relief/$nino", Seq(), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listLossClaimsResult(connector) shouldBe expected
      }
    }

    "provided with a tax year parameter" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Left(DesResponse(correlationId, ListLossClaimsResponse(Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`),
                                                                                  LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/claims-for-relief/$nino", Seq(("taxYear", "2019")), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listLossClaimsResult(connector, taxYear = Some(DesTaxYear("2019"))) shouldBe expected
      }
    }

    "provided with a income source id parameter" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Left(DesResponse(correlationId, ListLossClaimsResponse(Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`),
                                                                                  LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/claims-for-relief/$nino", Seq(("incomeSourceId", "testId")), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listLossClaimsResult(connector, selfEmploymentId = Some("testId")) shouldBe expected
      }
    }

    "provided with a income source type parameter" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Left(DesResponse(correlationId, ListLossClaimsResponse(Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`),
                                                                                  LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/claims-for-relief/$nino", Seq(("incomeSourceType", "02")), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listLossClaimsResult(connector, incomeSourceType = Some(IncomeSourceType.`02`)) shouldBe expected
      }
    }

    "provided with a claim type parameter" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Left(DesResponse(correlationId, ListLossClaimsResponse(Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`),
                                                                                  LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/claims-for-relief/$nino", Seq(("claimType", "carry-sideways")), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listLossClaimsResult(connector, claimType = Some(TypeOfClaim.`carry-sideways`)) shouldBe expected
      }
    }

    "provided with all parameters" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Left(DesResponse(correlationId, ListLossClaimsResponse(Seq(LossClaimId(claimId, Some(1), TypeOfClaim.`carry-sideways`),
                                                                                  LossClaimId(claimId2, Some(2), TypeOfClaim.`carry-sideways`)))))

        MockedHttpClient
          .parameterGet(
            s"$baseUrl/income-tax/claims-for-relief/$nino",
            Seq(("taxYear", "2019"), ("incomeSourceId", "testId"), ("incomeSourceType", "01"), ("claimType", "carry-sideways")),
            desRequestHeaders: _*
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector,
          taxYear = Some(DesTaxYear("2019")),
          selfEmploymentId = Some("testId"),
          incomeSourceType = Some(IncomeSourceType.`01`),
          claimType = Some(TypeOfClaim.`carry-sideways`)) shouldBe
          expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(DesResponse(correlationId, SingleError(NinoFormatError)))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/claims-for-relief/$nino", Seq(), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listLossClaimsResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/claims-for-relief/$nino", Seq(("taxYear", "2019")), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listLossClaimsResult(connector, Some(DesTaxYear("2019"))) shouldBe expected
      }
    }

    def listLossClaimsResult(connector: LossClaimConnector,
                             taxYear: Option[DesTaxYear] = None,
                             incomeSourceType: Option[IncomeSourceType] = None,
                             selfEmploymentId: Option[String] = None,
                             claimType: Option[TypeOfClaim] = None): DesOutcome[ListLossClaimsResponse[LossClaimId]] =
      await(
        connector.listLossClaims(
          ListLossClaimsRequest(
            nino = Nino(nino),
            taxYear = taxYear,
            incomeSourceType = incomeSourceType,
            selfEmploymentId = selfEmploymentId,
            claimType = claimType
          )))
  }
}
