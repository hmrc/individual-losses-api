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
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v4.models.domain.lossClaim.TypeOfClaim
import v4.models.request.amendLossClaimsOrder.{AmendLossClaimsOrderRequestBody, AmendLossClaimsOrderRequestData, Claim}

import scala.concurrent.Future

class AmendLossClaimsOrderConnectorSpec extends ConnectorSpec {

  val nino: String     = "AA123456A"
  val taxYear: TaxYear = TaxYear.fromMtd("2023-24")

  val amendLossClaimsOrder: AmendLossClaimsOrderRequestBody = AmendLossClaimsOrderRequestBody(
    typeOfClaim = TypeOfClaim.`carry-sideways`,
    listOfLossClaims = Seq(
      Claim("1234568790ABCDE", 1),
      Claim("1234568790ABCDF", 2)
    )
  )

  trait Test {
    _: ConnectorTest =>

    val request: AmendLossClaimsOrderRequestData = AmendLossClaimsOrderRequestData(
      nino = Nino(nino),
      taxYearClaimedFor = taxYear,
      body = amendLossClaimsOrder
    )

    val connector: AmendLossClaimsConnector = new AmendLossClaimsConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)
  }

  "amendLossClaimsOrder" when {
    "given a valid request" should {
      "return a success response when feature switch is disabled (TysIfs enabled)" in new IfsTest with Test {
        private val expected = Right(ResponseWrapper(correlationId, ()))

        MockedSharedAppConfig.featureSwitchConfig returns Configuration("ifs_hip_migration_1793.enabled" -> false)

        willPut(
          url = url"$baseUrl/income-tax/claims-for-relief/preferences/23-24/$nino",
          body = amendLossClaimsOrder
        ).returns(Future.successful(expected))

        val result: DownstreamOutcome[Unit] = await(connector.amendLossClaimsOrder(request))
        result shouldBe expected
      }

      "return a success response when feature switch is enabled (HIP enabled)" in new HipTest with Test {
        private val expected = Right(ResponseWrapper(correlationId, ()))

        MockedSharedAppConfig.featureSwitchConfig returns Configuration("ifs_hip_migration_1793.enabled" -> true)

        willPut(
          url = url"$baseUrl/itsd/income-sources/claims-for-relief/$nino/preferences?taxYear=23-24",
          body = amendLossClaimsOrder
        ).returns(Future.successful(expected))

        val result: DownstreamOutcome[Unit] = await(connector.amendLossClaimsOrder(request))
        result shouldBe expected
      }
    }
  }

}
