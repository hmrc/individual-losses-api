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

package v3.connectors

import api.connectors.{ConnectorSpec, DownstreamOutcome}
import api.models.ResponseWrapper
import api.models.domain.Nino
import v3.models.request.deleteLossClaim.DeleteLossClaimRequest

import scala.concurrent.Future

class DeleteLossClaimConnectorSpec extends ConnectorSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890ag"

  trait Test {
    _: ConnectorTest =>

    val connector: DeleteLossClaimConnector = new DeleteLossClaimConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

  "delete LossClaim" when {

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new DesTest with Test {
        val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

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

    def deleteLossClaimResult(connector: DeleteLossClaimConnector): DownstreamOutcome[Unit] =
      await(
        connector.deleteLossClaim(DeleteLossClaimRequest(nino = Nino(nino), claimId = claimId))
      )
  }

}
