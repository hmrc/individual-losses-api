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

package v5.lossClaims.amendType

import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{Nino, Timestamp}
import shared.models.outcomes.ResponseWrapper
import v5.lossClaims.amendType.def1.model.request.{Def1_AmendLossClaimTypeRequestBody, Def1_AmendLossClaimTypeRequestData}
import v5.lossClaims.amendType.def1.model.response.Def1_AmendLossClaimTypeResponse
import v5.lossClaims.amendType.model.response.AmendLossClaimTypeResponse
import v5.lossClaims.common.models.{ClaimId, TypeOfClaim, TypeOfLoss}

import scala.concurrent.Future

class AmendLossClaimTypeConnectorSpec extends ConnectorSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890ag"

  "amendLossClaimType" when {

    val response: AmendLossClaimTypeResponse = Def1_AmendLossClaimTypeResponse(
      businessId = "XKIS00000000988",
      typeOfLoss = TypeOfLoss.`self-employment`,
      typeOfClaim = TypeOfClaim.`carry-forward`,
      taxYearClaimedFor = "2019-20",
      sequence = Some(1),
      lastModified = Timestamp("2018-07-13T12:13:48.763Z")
    )

    val amendLossClaimType: Def1_AmendLossClaimTypeRequestBody = Def1_AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        val expected: Right[Nothing, ResponseWrapper[AmendLossClaimTypeResponse]] = Right(ResponseWrapper(correlationId, response))

        willPut(s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId", amendLossClaimType)
          .returning(Future.successful(expected))

        val result: DownstreamOutcome[AmendLossClaimTypeResponse] = amendLossClaimTypeResult(connector)
        result shouldBe expected
      }
    }

    def amendLossClaimTypeResult(connector: AmendLossClaimTypeConnector): DownstreamOutcome[AmendLossClaimTypeResponse] =
      await(
        connector.amendLossClaimType(
          Def1_AmendLossClaimTypeRequestData(
            nino = Nino(nino),
            claimId = ClaimId(claimId),
            amendLossClaimType
          )))
  }

  trait Test { _: ConnectorTest =>
    val connector: AmendLossClaimTypeConnector = new AmendLossClaimTypeConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)
  }

}
