/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.retrieve

import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v7.lossesAndClaims.retrieve.model.request.RetrieveLossesAndClaimsRequestData
import v7.lossesAndClaims.retrieve.model.response.PreferenceOrderEnum.`carry-back`
import v7.lossesAndClaims.retrieve.model.response.{
  CarryBack,
  CarryForward,
  CarrySideways,
  Claims,
  Losses,
  PreferenceOrder,
  RetrieveLossesAndClaimsResponse
}

import scala.concurrent.Future

class RetrieveLossesAndClaimsConnectorSpec extends ConnectorSpec {

  private val nino: String       = "AA123456A"
  private val businessId: String = "X0IS12345678901"
  private val taxYear: String    = "2026-27"

  val retrieveResponse: RetrieveLossesAndClaimsResponse = RetrieveLossesAndClaimsResponse(
    "2026-08-24T14:15:22.544Z",
    Some(
      Claims(
        Some(
          CarryBack(
            Some(5000.99),
            Some(5000.99),
            Some(5000.99)
          )),
        Some(
          CarrySideways(
            Some(5000.99)
          )),
        Some(
          PreferenceOrder(
            Some(`carry-back`)
          )),
        Some(
          CarryForward(
            Some(5000.99),
            Some(5000.99)
          ))
      )),
    Some(
      Losses(
        Some(5000.99)
      ))
  )

  val request = RetrieveLossesAndClaimsRequestData(Nino(nino), BusinessId(businessId), TaxYear.fromMtd(taxYear))

  def retrieveLossClaimResult(connector: RetrieveLossesAndClaimsConnector): DownstreamOutcome[RetrieveLossesAndClaimsResponse] =
    await(connector.retrieveLossesAndClaims(request))

  "retrieveLossesAndClaims" must {
    "return a success response" in new HipTest with Test {

      private val expected = Left(ResponseWrapper(correlationId, retrieveResponse))

      willGet(url = url"$baseUrl/itsd/reliefs/loss-claims/$nino/$businessId?taxYear=26-27")
        .returning(Future.successful(expected))

      val result: DownstreamOutcome[RetrieveLossesAndClaimsResponse] = retrieveLossClaimResult(connector)
      result shouldBe expected
    }
  }

  trait Test {
    self: ConnectorTest =>
    val connector: RetrieveLossesAndClaimsConnector = new RetrieveLossesAndClaimsConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)
  }

}
