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

package v4.connectors

import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import v4.models.domain.lossClaim.TypeOfClaim
import v4.models.request.amendLossClaimsOrder.{AmendLossClaimsOrderRequestBody, AmendLossClaimsOrderRequestData, Claim}

import scala.concurrent.Future

class AmendLossClaimsOrderConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

  val amendLossClaimsOrder: AmendLossClaimsOrderRequestBody = AmendLossClaimsOrderRequestBody(
    typeOfClaim = TypeOfClaim.`carry-sideways`,
    listOfLossClaims = Seq(
      Claim("1234568790ABCDE", 1),
      Claim("1234568790ABCDF", 2)
    )
  )

  trait Test {
    _: ConnectorTest =>

    def taxYear: TaxYear

    val request: AmendLossClaimsOrderRequestData = AmendLossClaimsOrderRequestData(
      nino = Nino(nino),
      taxYearClaimedFor = taxYear,
      body = amendLossClaimsOrder
    )

    val connector: AmendLossClaimsConnector = new AmendLossClaimsConnector(http = mockHttpClient, appConfig = mockAppConfig)
  }

  "amendLossClaimsOrder" when {
    "given a tax year prior to 2023-24" should {
      "return a success response" in new TysIfsTest with Test {
        def taxYear: TaxYear = TaxYear.fromMtd("2022-23")

        private val expected = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = s"$baseUrl/income-tax/claims-for-relief/preferences/22-23/$nino",
          body = amendLossClaimsOrder
        ).returns(Future.successful(expected))

        val result: DownstreamOutcome[Unit] = await(connector.amendLossClaimsOrder(request))
        result shouldBe expected
      }
    }

    "given a 2023-24 tax year" should {
      "return a success response" in new TysIfsTest with Test {
        def taxYear: TaxYear = TaxYear.fromMtd("2023-24")

        val expected = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = s"$baseUrl/income-tax/claims-for-relief/preferences/23-24/$nino",
          body = amendLossClaimsOrder
        ).returns(Future.successful(expected))

        await(connector.amendLossClaimsOrder(request)) shouldBe expected
      }
    }
  }

}
