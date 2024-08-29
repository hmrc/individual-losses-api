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

package v5.lossClaims.delete

import api.connectors.{ConnectorSpec, DownstreamOutcome}
import api.models.domain.Nino
import api.models.outcomes.ResponseWrapper
import play.api.Configuration
import v4.models.domain.lossClaim.ClaimId
import v5.lossClaims.delete.def1.model.request.Def1_DeleteLossClaimRequestData

import scala.concurrent.Future

class DeleteLossClaimConnectorSpec extends ConnectorSpec {

  private val nino    = Nino("AA123456A")
  private val claimId = ClaimId("AAZZ1234567890ag")

  "delete LossClaim" when {
    "a valid request is supplied" when {
      "Des is not migrated to HIP" must {
        "return a successful response " in new DesTest with Test {
          MockedAppConfig.featureSwitches returns Configuration("des_hip_migration_1509.enabled" -> false)
          val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

          MockedHttpClient
            .delete(
              url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
              config = dummyHeaderCarrierConfig,
              requiredHeaders = requiredDesHeaders,
              excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
            )
            .returns(Future.successful(expected))
          val result: DownstreamOutcome[Unit] = await(connector.deleteLossClaim(request))

          result shouldBe expected
        }
      }
      "Des is migrated to HIP" must {
        "return a successful response " in new HipTest with Test {
          MockedAppConfig.featureSwitches returns Configuration("des_hip_migration_1509.enabled" -> true)
          val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

          MockedHttpClient
            .delete(
              url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
              config = dummyHeaderCarrierConfig,
              requiredHeaders = requiredHeaders,
              excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
            )
            .returns(Future.successful(expected))
          val result: DownstreamOutcome[Unit] = await(connector.deleteLossClaim(request))

          result shouldBe expected
        }
      }
    }

  }

  trait Test {
    _: ConnectorTest =>

    val connector: DeleteLossClaimConnector = new DeleteLossClaimConnector(http = mockHttpClient, appConfig = mockAppConfig)

    protected val request: Def1_DeleteLossClaimRequestData = Def1_DeleteLossClaimRequestData(nino = nino, claimId = claimId)

  }

}
