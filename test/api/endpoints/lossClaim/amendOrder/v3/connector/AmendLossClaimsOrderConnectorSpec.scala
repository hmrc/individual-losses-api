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

package api.endpoints.lossClaim.amendOrder.v3.connector

import api.connectors.ConnectorSpec
import api.endpoints.lossClaim.amendOrder.v3.model.Claim
import api.endpoints.lossClaim.amendOrder.v3.request.{AmendLossClaimsOrderRequest, AmendLossClaimsOrderRequestBody}
import api.endpoints.lossClaim.connector.v3.LossClaimConnector
import api.endpoints.lossClaim.domain.v3.TypeOfClaim
import api.models.ResponseWrapper
import api.models.domain.{Nino, TaxYear}

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

    val request: AmendLossClaimsOrderRequest = AmendLossClaimsOrderRequest(
      nino = Nino(nino),
      taxYearClaimedFor = taxYear,
      body = amendLossClaimsOrder
    )

    val connector: LossClaimConnector = new LossClaimConnector(http = mockHttpClient, appConfig = mockAppConfig)
  }

  "amendLossClaimsOrderV3" when {
    "a valid non-TYS request is supplied" should {
      "return a successful response with the correct correlationId" in new DesTest with Test {
        def taxYear: TaxYear = TaxYear.fromMtd("2022-23")
        val expected = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = s"$baseUrl/income-tax/claims-for-relief/$nino/preferences/2023",
          body = amendLossClaimsOrder
        ).returns(Future.successful(expected))

        await(connector.amendLossClaimsOrder(request)) shouldBe expected
      }
    }

    "a valid TYS specific request is supplied" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
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