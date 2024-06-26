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

package v5.lossClaims.retrieve

import api.connectors.{ConnectorSpec, DownstreamOutcome}
import api.models.domain.{Nino, Timestamp}
import api.models.outcomes.ResponseWrapper
import v4.models.domain.lossClaim.{ClaimId, TypeOfClaim, TypeOfLoss}
import v5.lossClaims.retrieve.def1.model.request.Def1_RetrieveLossClaimRequestData
import v5.lossClaims.retrieve.def1.model.response.Def1_RetrieveLossClaimResponse
import v5.lossClaims.retrieve.model.response.RetrieveLossClaimResponse

import scala.concurrent.Future

class RetrieveLossClaimConnectorSpec extends ConnectorSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890ag"

  "retrieve LossClaim" should {
    val validTaxYear: String    = "2019-20"
    val validBusinessId: String = "XAIS01234567890"
    val nino: String            = "AA123456A"
    val claimId: String         = "AAZZ1234567890a"

    val retrieveResponse: RetrieveLossClaimResponse = Def1_RetrieveLossClaimResponse(
      validTaxYear,
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      validBusinessId,
      Some(1),
      Timestamp("2018-07-13T12:13:48.763Z")
    )
    val request = Def1_RetrieveLossClaimRequestData(Nino(nino), ClaimId(claimId))

    def retrieveLossClaimResult(connector: RetrieveLossClaimConnector): DownstreamOutcome[RetrieveLossClaimResponse] = {
      await(
        connector.retrieveLossClaim(request)
      )
    }

    "return a successful response and correlationId" when {
      "provided with a valid request" in new IfsTest with Test {

        val expected: Left[ResponseWrapper[RetrieveLossClaimResponse], Nothing] = Left(ResponseWrapper(correlationId, retrieveResponse))

        MockedHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        retrieveLossClaimResult(connector) shouldBe expected
      }
    }
  }

  trait Test {
    _: ConnectorTest =>

    val connector: RetrieveLossClaimConnector = new RetrieveLossClaimConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

}
