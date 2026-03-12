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

package v6.lossClaims.amendOrder

import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v6.lossClaims.amendOrder.def1.model.request.{Claim, Def1_AmendLossClaimsOrderRequestBody, Def1_AmendLossClaimsOrderRequestData}
import v6.lossClaims.common.models.TypeOfClaim

import scala.concurrent.Future

class AmendLossClaimsOrderConnectorSpec extends ConnectorSpec {

  val nino: String     = "AA123456A"
  val taxYear: TaxYear = TaxYear.fromMtd("2023-24")

  val amendLossClaimsOrder: Def1_AmendLossClaimsOrderRequestBody = Def1_AmendLossClaimsOrderRequestBody(
    typeOfClaim = TypeOfClaim.`carry-sideways`,
    listOfLossClaims = Seq(
      Claim("1234568790ABCDE", 1),
      Claim("1234568790ABCDF", 2)
    )
  )

  "amendLossClaimsOrder" when {
    "given a valid request" should {
      "return a success response" in new HipTest with Test {
        private val expected = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = url"$baseUrl/itsd/income-sources/claims-for-relief/$nino/preferences?taxYear=23-24",
          body = amendLossClaimsOrder
        ).returning(Future.successful(expected))

        val result: DownstreamOutcome[Unit] = await(connector.amendLossClaimsOrder(request))
        result shouldBe expected
      }
    }
  }

  trait Test {
    self: ConnectorTest =>

    val request: Def1_AmendLossClaimsOrderRequestData = Def1_AmendLossClaimsOrderRequestData(
      nino = Nino(nino),
      taxYearClaimedFor = taxYear,
      body = amendLossClaimsOrder
    )

    val connector: AmendLossClaimsOrderConnector =
      new AmendLossClaimsOrderConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)

  }

}
