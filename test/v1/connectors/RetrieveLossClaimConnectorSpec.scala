/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDateTime

import v1.models.des._
import v1.models.domain._
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData._

import scala.concurrent.Future

class RetrieveLossClaimConnectorSpec extends LossClaimConnectorSpec {

  "retrieve LossClaim" should {

    val testDateTime: LocalDateTime = LocalDateTime.now()
    val validTaxYear: String = "2019-20"
    val validSelfEmploymentId: String = "XAIS01234567890"
    val nino: String = "AA123456A"
    val claimId: String = "AAZZ1234567890a"

    val retrieveResponse: LossClaimResponse = LossClaimResponse(
      Some(validSelfEmploymentId),
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      validTaxYear,
      testDateTime.toString
    )

    def retrieveLossClaimResult(connector: LossClaimConnector): DesOutcome[LossClaimResponse] = {
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

      "provided with a valid request" in new Test {
        val expected = Left(DesResponse(correlationId, retrieveResponse))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(expected))

        retrieveLossClaimResult(connector) shouldBe expected
      }
    }

    "return an unsuccessful response" when {

      "provided with a single error" in new Test {
        val expected = Left(DesResponse(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .get(s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(expected))

        retrieveLossClaimResult(connector) shouldBe expected
      }

      "provided with multiple errors" in new Test {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, ClaimIdFormatError))))

        MockHttpClient
          .get(s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(expected))

        retrieveLossClaimResult(connector) shouldBe expected
      }
    }
  }
}