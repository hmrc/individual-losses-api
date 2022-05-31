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

package api.endpoints.lossClaim.delete.v3.connector

import api.connectors.DownstreamOutcome
import api.endpoints.lossClaim.connector.v3.{ LossClaimConnector, LossClaimConnectorSpec }
import api.endpoints.lossClaim.delete.v3.request.DeleteLossClaimRequest
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._

import scala.concurrent.Future

class DeleteLossClaimConnectorSpec extends LossClaimConnectorSpec {

  "delete LossClaim" when {

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new DesTest {
        val expected = Right(ResponseWrapper(correlationId, ()))

        MockHttpClient
          .delete(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        deleteLossClaimResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new DesTest {

        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .delete(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        deleteLossClaimResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new DesTest {

        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, ClaimIdFormatError))))

        MockHttpClient
          .delete(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        deleteLossClaimResult(connector) shouldBe expected
      }
    }

    def deleteLossClaimResult(connector: LossClaimConnector): DownstreamOutcome[Unit] =
      await(
        connector.deleteLossClaim(DeleteLossClaimRequest(nino = Nino(nino), claimId = claimId))
      )
  }
}