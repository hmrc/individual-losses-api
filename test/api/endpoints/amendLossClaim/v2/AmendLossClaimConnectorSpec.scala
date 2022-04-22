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

package api.endpoints.amendLossClaim.v2

import api.connectors.DownstreamOutcome
import api.connectors.v2.{ LossClaimConnector, LossClaimConnectorSpec }
import api.endpoints.amendLossClaim.v2.request.AmendLossClaimRequest
import api.endpoints.common.lossClaim.v2.domain.{ AmendLossClaim, TypeOfClaim, TypeOfLoss }
import api.models.domain.Nino
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.models.response.lossClaim.v2.LossClaimResponse
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.Future

class AmendLossClaimConnectorSpec extends LossClaimConnectorSpec {

  "amend LossClaim" when {

    val amendLossClaimResponse: LossClaimResponse = LossClaimResponse(
      businessId = Some("XKIS00000000988"),
      typeOfLoss = TypeOfLoss.`self-employment`,
      typeOfClaim = TypeOfClaim.`carry-forward`,
      taxYear = "2019-20",
      lastModified = LocalDateTime.now().toString
    )

    val amendLossClaim: AmendLossClaim = AmendLossClaim(TypeOfClaim.`carry-forward`)

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))

    val requiredDesHeadersPut: Seq[(String, String)] = requiredDesHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(ResponseWrapper(correlationId, amendLossClaimResponse))

        MockHttpClient
          .put(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyDesHeaderCarrierConfig,
            body = amendLossClaim,
            requiredHeaders = requiredDesHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        amendLossClaimResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .put(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyDesHeaderCarrierConfig,
            body = amendLossClaim,
            requiredHeaders = requiredDesHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        amendLossClaimResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, ClaimIdFormatError))))

        MockHttpClient
          .put(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyDesHeaderCarrierConfig,
            body = amendLossClaim,
            requiredHeaders = requiredDesHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        amendLossClaimResult(connector) shouldBe expected
      }
    }

    def amendLossClaimResult(connector: LossClaimConnector): DownstreamOutcome[LossClaimResponse] =
      await(
        connector.amendLossClaim(
          AmendLossClaimRequest(
            nino = Nino(nino),
            claimId = claimId,
            amendLossClaim
          )))
  }
}
