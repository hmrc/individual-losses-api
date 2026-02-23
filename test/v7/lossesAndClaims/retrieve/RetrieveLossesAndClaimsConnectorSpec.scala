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
import v7.lossesAndClaims.retrieve.fixtures.RetrieveLossesAndClaimsFixtures.responseBodyModel
import v7.lossesAndClaims.retrieve.model.request.RetrieveLossesAndClaimsRequestData
import v7.lossesAndClaims.retrieve.model.response.RetrieveLossesAndClaimsResponse

import scala.concurrent.Future

class RetrieveLossesAndClaimsConnectorSpec extends ConnectorSpec {

  private val nino: String       = "AA123456A"
  private val businessId: String = "X0IS12345678901"
  private val taxYear: String    = "2026-27"

  private val request: RetrieveLossesAndClaimsRequestData = RetrieveLossesAndClaimsRequestData(
    nino = Nino(nino),
    businessId = BusinessId(businessId),
    taxYear = TaxYear.fromMtd(taxYear)
  )

  "retrieveLossesAndClaims" must {
    "return a success response" in new HipTest with Test {
      private val expected = Left(ResponseWrapper(correlationId, responseBodyModel))

      willGet(url = url"$baseUrl/itsd/reliefs/loss-claims/$nino/$businessId?taxYear=26-27")
        .returning(Future.successful(expected))

      val result: DownstreamOutcome[RetrieveLossesAndClaimsResponse] = await(connector.retrieveLossesAndClaims(request))
      result shouldBe expected
    }
  }

  private trait Test {
    self: ConnectorTest =>
    val connector: RetrieveLossesAndClaimsConnector = new RetrieveLossesAndClaimsConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)
  }

}
