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
import shared.models.domain.Nino
import shared.models.domain.TaxYear.currentTaxYear
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v4.models.domain.bfLoss.LossId
import v4.models.request.deleteBFLosses.DeleteBFLossRequestData

import scala.concurrent.Future

class DeleteBFLossConnectorSpec extends ConnectorSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val request: DeleteBFLossRequestData = DeleteBFLossRequestData(nino = Nino(nino), lossId = LossId(lossId))

  "deleteBFLosses" when {
    "given a valid request" when {
      "HIP is pointed to ITSA" must {
        "return a success response" in new HipTest with Test {
          MockedSharedAppConfig.featureSwitchConfig returns Configuration("hipItsa_hipItsd_migration_1504.enabled" -> false)

          val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

          willDelete(url = url"$baseUrl/itsa/income-tax/v1/brought-forward-losses/$nino/${currentTaxYear.asTysDownstream}/$lossId")
            .returning(Future.successful(expected))

          val result: DownstreamOutcome[Unit] = await(connector.deleteBFLoss(request))
          result shouldBe expected
        }
      }
      "HIP is pointed to ITSD" must {
        "return a success response" in new HipTest with Test {
          MockedSharedAppConfig.featureSwitchConfig returns Configuration("hipItsa_hipItsd_migration_1504.enabled" -> true)

          val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

          willDelete(url = url"$baseUrl/itsd/income-sources/brought-forward-losses/$nino/$lossId?taxYear=${currentTaxYear.asTysDownstream}")
            .returning(Future.successful(expected))

          val result: DownstreamOutcome[Unit] = await(connector.deleteBFLoss(request))
          result shouldBe expected
        }
      }
    }
  }

  trait Test { self: ConnectorTest =>
    val connector: DeleteBFLossConnector = new DeleteBFLossConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)
  }

}
