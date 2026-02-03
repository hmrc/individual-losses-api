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

package v7.lossesAndClaims.createAmend

import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.errors.{DownstreamErrorCode, DownstreamErrors}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v7.lossesAndClaims.commons.PreferenceOrderEnum.`carry-back`
import v7.lossesAndClaims.commons.{Losses, PreferenceOrder}
import v7.lossesAndClaims.createAmend.request.*

import scala.concurrent.Future

class CreateAmendLossesAndClaimsConnectorSpec extends ConnectorSpec {

  private val nino: String       = "AA123456A"
  private val businessId: String = "X0IS12345678901"
  private val taxYear: String    = "2026-27"

  val createAmendLossesAndClaimsRequestBody: CreateAmendLossesAndClaimsRequestBody = CreateAmendLossesAndClaimsRequestBody(
    Option(
      Claims(
        Option(
          CarryBack(
            Option(5000.99),
            Option(5000.99),
            Option(5000.99)
          )),
        Option(
          CarrySideways(
            Option(5000.99)
          )),
        Option(
          PreferenceOrder(
            Option(`carry-back`)
          )),
        Option(
          CarryForward(
            Option(5000.99),
            Option(5000.99)
          ))
      )),
    Option(
      Losses(
        Option(5000.99)
      ))
  )

  val request =
    CreateAmendLossesAndClaimsRequestData(Nino(nino), BusinessId(businessId), TaxYear.fromMtd(taxYear), createAmendLossesAndClaimsRequestBody)

  "createAmendLossesAndClaims" must {
    "return a success response" in new HipTest with Test {

      val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

      willPut(url = url"$baseUrl/itsd/reliefs/loss-claims/$nino/$businessId?taxYear=26-27", createAmendLossesAndClaimsRequestBody)
        .returning(Future.successful(expected))

      val result: DownstreamOutcome[Unit] = await(connector.createAmendLossClaimsAndLosses(request))
      result shouldBe expected
    }

    "return an unsuccessful response" when {
      "the downstream request is unsuccessful" in new HipTest with Test {
        val downstreamErrorResponse: DownstreamErrors                 = DownstreamErrors.single(DownstreamErrorCode("SOME_ERROR"))
        val outcome: Left[ResponseWrapper[DownstreamErrors], Nothing] = Left(ResponseWrapper(correlationId, downstreamErrorResponse))

        willPut(url = url"$baseUrl/itsd/reliefs/loss-claims/$nino/$businessId?taxYear=26-27", createAmendLossesAndClaimsRequestBody)
          .returns(Future.successful(outcome))

        val result: DownstreamOutcome[Unit] = await(connector.createAmendLossClaimsAndLosses(request))
        result shouldBe outcome
      }
    }
  }

  trait Test {
    self: ConnectorTest =>

    val connector: CreateAmendLossesAndClaimsConnector =
      new CreateAmendLossesAndClaimsConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)

  }

}
