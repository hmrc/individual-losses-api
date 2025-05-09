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

package v5.lossClaims.create

import play.api.Configuration
import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.Nino
import shared.models.outcomes.ResponseWrapper
import v5.lossClaims.common.models._
import v5.lossClaims.create.def1.model.request.{Def1_CreateLossClaimRequestBody, Def1_CreateLossClaimRequestData}
import v5.lossClaims.create.def1.model.response.Def1_CreateLossClaimResponse
import v5.lossClaims.create.model.response.CreateLossClaimResponse

import scala.concurrent.Future

class CreateLossClaimConnectorSpec extends ConnectorSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890ag"

  "create LossClaim" when {

    val lossClaim: Def1_CreateLossClaimRequestBody = Def1_CreateLossClaimRequestBody(
      taxYearClaimedFor = "2019-20",
      typeOfLoss = TypeOfLoss.`self-employment`,
      typeOfClaim = TypeOfClaim.`carry-forward`,
      businessId = "XKIS00000000988"
    )

    "a valid request is supplied" should {
      "return a successful IFS response with the correct correlationId" in new IfsTest with Test {
        val expected: Right[Nothing, ResponseWrapper[CreateLossClaimResponse]] =
          Right(ResponseWrapper(correlationId, Def1_CreateLossClaimResponse(claimId)))
        MockedSharedAppConfig.featureSwitchConfig.returns(Configuration("ifs_hip_migration_1505.enabled" -> false))

        willPost(s"$baseUrl/income-tax/claims-for-relief/$nino", lossClaim)
          .returning(Future.successful(expected))

        val result: DownstreamOutcome[CreateLossClaimResponse] = await(
          connector.createLossClaim(
            Def1_CreateLossClaimRequestData(
              nino = Nino(nino),
              lossClaim
            ))
        )
        result shouldBe expected
      }

      "return a successful HIP response with the correct correlationId" in new HipTest with Test {
        val expected: Right[Nothing, ResponseWrapper[CreateLossClaimResponse]] =
          Right(ResponseWrapper(correlationId, Def1_CreateLossClaimResponse(claimId)))
        MockedSharedAppConfig.featureSwitchConfig.returns(Configuration("ifs_hip_migration_1505.enabled" -> true))

        willPost(s"$baseUrl/itsd/income-sources/claims-for-relief/$nino", lossClaim)
          .returning(Future.successful(expected))

        val result: DownstreamOutcome[CreateLossClaimResponse] = await(
          connector.createLossClaim(
            Def1_CreateLossClaimRequestData(
              nino = Nino(nino),
              lossClaim
            ))
        )
        result shouldBe expected
      }
    }
  }

  trait Test { _: ConnectorTest =>
    val connector: CreateLossClaimConnector = new CreateLossClaimConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)
  }

}
