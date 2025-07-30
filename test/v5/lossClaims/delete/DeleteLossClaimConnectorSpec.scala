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

package v5.lossClaims.delete

import play.api.Configuration
import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v5.lossClaims.common.models.ClaimId
import v5.lossClaims.delete.def1.model.request.Def1_DeleteLossClaimRequestData

import scala.concurrent.Future

class DeleteLossClaimConnectorSpec extends ConnectorSpec {

  private val nino              = Nino("AA123456A")
  private val claimId           = ClaimId("AAZZ1234567890ag")
  private val taxYearClaimedFor = TaxYear.fromMtd("2019-20")

  "delete LossClaim" when {
    "given a valid request" when {
      "the feature switch is disabled (ITSA enabled)" should {
        "return a successful response " in new HipTest with Test {
          MockedSharedAppConfig.featureSwitchConfig returns Configuration("hipItsa_hipItsd_migration_1509.enabled" -> false)

          val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

          willDelete(url"$baseUrl/itsa/income-tax/v1/claims-for-relief/$nino/19-20/$claimId")
            .returning(Future.successful(expected))

          val result: DownstreamOutcome[Unit] = await(connector.deleteLossClaim(request, taxYearClaimedFor))
          result shouldBe expected
        }
      }

      "the feature switch is disabled (ITSD enabled)" should {
        "return a successful response" in new HipTest with Test {
          MockedSharedAppConfig.featureSwitchConfig returns Configuration("hipItsa_hipItsd_migration_1509.enabled" -> true)
          val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))
          willDelete(url"$baseUrl/itsd/income-sources/claims-for-relief/$nino/$claimId?taxYear=19-20")
            .returning(Future.successful(expected))

          val result: DownstreamOutcome[Unit] = await(connector.deleteLossClaim(request, taxYearClaimedFor))
          result shouldBe expected
        }
      }
    }

  }

  trait Test { self: ConnectorTest =>
    val connector: DeleteLossClaimConnector = new DeleteLossClaimConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)

    val request: Def1_DeleteLossClaimRequestData = Def1_DeleteLossClaimRequestData(nino = nino, claimId = claimId)
  }

}
