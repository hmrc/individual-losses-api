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

package v3.connectors

import api.connectors.DownstreamOutcome
import api.connectors.v3.LossClaimConnector
import api.models.domain.Nino
import api.models.domain.lossClaim.v3.{TypeOfClaim, TypeOfLoss}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import v3.models.request.retrieveLossClaim.RetrieveLossClaimRequest
import v3.models.response.retrieveLossClaim.RetrieveLossClaimResponse

import java.time.LocalDateTime
import scala.concurrent.Future

class RetrieveLossClaimConnectorSpec extends LossClaimConnectorSpec {

  "retrieve LossClaim" should {

    val testDateTime: LocalDateTime = LocalDateTime.now()
    val validTaxYear: String        = "2019-20"
    val validBusinessId: String     = "XAIS01234567890"
    val nino: String                = "AA123456A"
    val claimId: String             = "AAZZ1234567890a"

    val retrieveResponse: RetrieveLossClaimResponse = RetrieveLossClaimResponse(
      validTaxYear,
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      validBusinessId,
      Some(1),
      testDateTime.toString
    )

    def retrieveLossClaimResult(connector: LossClaimConnector): DownstreamOutcome[RetrieveLossClaimResponse] = {
      await(
        connector.retrieveLossClaim(
          RetrieveLossClaimRequest(
            nino = Nino(nino),
            claimId = claimId
          )
        )
      )
    }

    "return a successful response and correlationId" when {

      "provided with a valid request" in new IfsTest {
        val expected = Left(ResponseWrapper(correlationId, retrieveResponse))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        retrieveLossClaimResult(connector) shouldBe expected
      }
    }

    "return an unsuccessful response" when {

      "provided with a single error" in new IfsTest {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        retrieveLossClaimResult(connector) shouldBe expected
      }

      "provided with multiple errors" in new IfsTest {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, ClaimIdFormatError))))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        retrieveLossClaimResult(connector) shouldBe expected
      }
    }
  }
}
