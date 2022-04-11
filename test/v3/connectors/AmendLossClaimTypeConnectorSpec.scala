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
import uk.gov.hmrc.http.HeaderCarrier
import v3.models.request.amendLossClaimType.{AmendLossClaimTypeRequest, AmendLossClaimTypeRequestBody}
import v3.models.response.amendLossClaimType.AmendLossClaimTypeResponse

import java.time.LocalDateTime
import scala.concurrent.Future

class AmendLossClaimTypeConnectorSpec extends LossClaimConnectorSpec {

  "amendLossClaimType" when {

    val response: AmendLossClaimTypeResponse = AmendLossClaimTypeResponse(
      businessId = "XKIS00000000988",
      typeOfLoss = TypeOfLoss.`self-employment`,
      typeOfClaim = TypeOfClaim.`carry-forward`,
      taxYearClaimedFor = "2019-20",
      lastModified = LocalDateTime.now().toString,
      sequence = Some(1)
    )

    val amendLossClaimType: AmendLossClaimTypeRequestBody = AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders)

    val requiredIfsHeadersPut: Seq[(String, String)] = requiredIfsHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new IfsTest {
        val expected = Right(ResponseWrapper(correlationId, response))

        MockHttpClient
          .put(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyIfsHeaderCarrierConfig,
            body = amendLossClaimType,
            requiredHeaders = requiredIfsHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        amendLossClaimTypeResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new IfsTest {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .put(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyIfsHeaderCarrierConfig,
            body = amendLossClaimType,
            requiredHeaders = requiredIfsHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        amendLossClaimTypeResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new IfsTest {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, ClaimIdFormatError))))

        MockHttpClient
          .put(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyIfsHeaderCarrierConfig,
            body = amendLossClaimType,
            requiredHeaders = requiredIfsHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        amendLossClaimTypeResult(connector) shouldBe expected
      }
    }

    def amendLossClaimTypeResult(connector: LossClaimConnector): DownstreamOutcome[AmendLossClaimTypeResponse] =
      await(
        connector.amendLossClaimType(
          AmendLossClaimTypeRequest(
            nino = Nino(nino),
            claimId = claimId,
            amendLossClaimType
          )))
  }
}
