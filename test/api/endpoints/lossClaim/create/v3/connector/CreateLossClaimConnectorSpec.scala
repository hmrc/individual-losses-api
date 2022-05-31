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

package api.endpoints.lossClaim.create.v3.connector

import api.connectors.DownstreamOutcome
import api.endpoints.lossClaim.connector.v3.{LossClaimConnector, LossClaimConnectorSpec}
import api.endpoints.lossClaim.create.v3.request.{CreateLossClaimRequest, CreateLossClaimRequestBody}
import api.endpoints.lossClaim.create.v3.response.CreateLossClaimResponse
import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class CreateLossClaimConnectorSpec extends LossClaimConnectorSpec {

  "create LossClaim" when {

    val lossClaim: CreateLossClaimRequestBody = CreateLossClaimRequestBody(
      taxYearClaimedFor = "2019-20",
      typeOfLoss = TypeOfLoss.`self-employment`,
      typeOfClaim = TypeOfClaim.`carry-forward`,
      businessId = "XKIS00000000988"
    )

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders)

    val requiredIfsHeadersPost: Seq[(String, String)] = requiredIfsHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new IfsTest {
        val expected = Right(ResponseWrapper(correlationId, CreateLossClaimResponse(claimId)))

        MockHttpClient
          .post(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            config = dummyIfsHeaderCarrierConfig,
            body = lossClaim,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        createLossClaimsResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new IfsTest {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .post(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            config = dummyIfsHeaderCarrierConfig,
            body = lossClaim,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        createLossClaimsResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new IfsTest {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockHttpClient
          .post(
            s"$baseUrl/income-tax/claims-for-relief/$nino",
            config = dummyIfsHeaderCarrierConfig,
            body = lossClaim,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

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