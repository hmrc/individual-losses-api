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

package v3.connectors

import uk.gov.hmrc.http.HeaderCarrier
import v3.models.downstream._
import v3.models.domain._
import v3.models.errors._
import v3.models.outcomes.ResponseWrapper
import v3.models.requestData._

import scala.concurrent.Future

class CreateLossClaimConnectorSpec extends LossClaimConnectorSpec {

  "create LossClaim" when {

    val lossClaim: LossClaim = LossClaim(
      taxYear = "2019-20",
      typeOfLoss = TypeOfLoss.`self-employment`,
      typeOfClaim = TypeOfClaim.`carry-forward`,
      businessId = Some("XKIS00000000988")
    )

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))

    val requiredIfsHeadersPost: Seq[(String, String)] = requiredIfsHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(ResponseWrapper(correlationId, CreateLossClaimResponse(claimId)))

        MockHttpClient
          .post(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            config = dummyIfsHeaderCarrierConfig,
            body = lossClaim,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(expected))

        createLossClaimsResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .post(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            config = dummyIfsHeaderCarrierConfig,
            body = lossClaim,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(expected))

        createLossClaimsResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockHttpClient
          .post(s"$baseUrl/income-tax/claims-for-relief/$nino",
            config = dummyIfsHeaderCarrierConfig,
            body = lossClaim,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(expected))

        createLossClaimsResult(connector) shouldBe expected
      }
    }

    def createLossClaimsResult(connector: LossClaimConnector): DownstreamOutcome[CreateLossClaimResponse] =
      await(
        connector.createLossClaim(
          CreateLossClaimRequest(
            nino = Nino(nino),
            lossClaim
          )))
  }
}