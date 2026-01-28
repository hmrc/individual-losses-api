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

package v7.lossesAndClaims.delete

import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v7.lossesAndClaims.delete.model.request.DeleteLossesAndClaimsRequestData

import scala.concurrent.Future

class DeleteLossesAndClaimsConnectorSpec extends ConnectorSpec {

  private val nino: String       = "AA123456A"
  private val businessId: String = "X0IS12345678901"
  private val taxYear: String    = "2025-26"

  val request = DeleteLossesAndClaimsRequestData(Nino(nino), BusinessId(businessId), TaxYear.fromMtd(taxYear))

  "deleteLossesAndClaims" must {
    "return a success response" in new HipTest with Test {

      val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

      willDelete(url = url"$baseUrl/itsd/reliefs/loss-claims/$nino/$businessId?taxYear=25-26")
        .returning(Future.successful(expected))

      val result: DownstreamOutcome[Unit] = await(connector.deleteLossesAndClaims(request))
      result shouldBe expected
    }
  }

  trait Test {
    self: ConnectorTest =>
    val connector: DeleteLossesAndClaimsConnector = new DeleteLossesAndClaimsConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)
  }

}
