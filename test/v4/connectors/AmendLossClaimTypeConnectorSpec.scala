/*
 * Copyright 2025 HM Revenue & Customs
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

package v4.connectors

import play.api.Configuration
import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{Nino, TaxYear, Timestamp}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import v4.models.domain.lossClaim.{ClaimId, TypeOfClaim, TypeOfLoss}
import v4.models.request.amendLossClaimType.{AmendLossClaimTypeRequestBody, AmendLossClaimTypeRequestData}
import v4.models.response.amendLossClaimType.AmendLossClaimTypeResponse

import scala.concurrent.Future

class AmendLossClaimTypeConnectorSpec extends ConnectorSpec {

  val nino: String              = "AA123456A"
  val claimId: String           = "AAZZ1234567890ag"
  val taxYearClaimedFor: String = "2019-20"

  trait Test {
    _: ConnectorTest =>

    val connector: AmendLossClaimTypeConnector = new AmendLossClaimTypeConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)

  }

  "amendLossClaimType" when {

    val response: AmendLossClaimTypeResponse = AmendLossClaimTypeResponse(
      businessId = "XKIS00000000988",
      typeOfLoss = TypeOfLoss.`self-employment`,
      typeOfClaim = TypeOfClaim.`carry-forward`,
      taxYearClaimedFor = taxYearClaimedFor,
      sequence = Some(1),
      lastModified = Timestamp("2018-07-13T12:13:48.763Z")
    )

    val amendLossClaimType: AmendLossClaimTypeRequestBody = AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = inputHeaders)

    "a valid request is supplied" should {
      "return a successful IFS response with the correct correlationId" in new IfsTest with Test {
        MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration("ifs_hip_migration_1506.enabled" -> false))
        val expected: Right[Nothing, ResponseWrapper[AmendLossClaimTypeResponse]] = Right(ResponseWrapper(correlationId, response))

        willPut(url"$baseUrl/income-tax/claims-for-relief/$nino/19-20/$claimId", amendLossClaimType).returning(Future.successful(expected))

        val result: DownstreamOutcome[AmendLossClaimTypeResponse] = amendLossClaimTypeResult(connector)
        result shouldBe expected
      }

      "return a successful HIP response with the correct correlationId" in new HipTest with Test {
        MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes().returns(Configuration("ifs_hip_migration_1506.enabled" -> true))
        val expected: Right[Nothing, ResponseWrapper[AmendLossClaimTypeResponse]] = Right(ResponseWrapper(correlationId, response))

        willPut(url"$baseUrl/itsd/income-sources/claims-for-relief/$nino/$claimId?taxYear=19-20", amendLossClaimType).returning(
          Future.successful(expected))

        val result: DownstreamOutcome[AmendLossClaimTypeResponse] = amendLossClaimTypeResult(connector)
        result shouldBe expected
      }
    }

    def amendLossClaimTypeResult(connector: AmendLossClaimTypeConnector): DownstreamOutcome[AmendLossClaimTypeResponse] =
      await(
        connector.amendLossClaimType(
          AmendLossClaimTypeRequestData(
            nino = Nino(nino),
            claimId = ClaimId(claimId),
            amendLossClaimType
          ),
          TaxYear.fromMtd(taxYearClaimedFor)
        )
      )
  }

}
