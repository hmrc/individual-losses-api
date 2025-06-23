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
import shared.models.domain.{Nino, Timestamp}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v4.models.domain.lossClaim.{ClaimId, TypeOfClaim, TypeOfLoss}
import v4.models.request.retrieveLossClaim.RetrieveLossClaimRequestData
import v4.models.response.retrieveLossClaim.RetrieveLossClaimResponse

import scala.concurrent.Future

class RetrieveLossClaimConnectorSpec extends ConnectorSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890ag"

  "retrieve LossClaim" when {
    val validTaxYear: String    = "2019-20"
    val validBusinessId: String = "XAIS01234567890"
    val nino: String            = "AA123456A"
    val claimId: String         = "AAZZ1234567890a"

    val retrieveResponse: RetrieveLossClaimResponse = RetrieveLossClaimResponse(
      validTaxYear,
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      validBusinessId,
      Some(1),
      Timestamp("2018-07-13T12:13:48.763Z")
    )

    val request: RetrieveLossClaimRequestData = RetrieveLossClaimRequestData(nino = Nino(nino), claimId = ClaimId(claimId))

    "the HIP feature switch is disabled (IFS enabled)" should {
      "return a successful response and correlationId" when {
        List(
          (false, false, None),
          (false, true, None),
          (true, false, None),
          (true, true, Some("AMEND_LOSS_CLAIM"))
        ).foreach { case (isAmendRequestValue, passIntentHeaderFlag, intentValue) =>
          s"provided with a valid request, isAmendRequest is $isAmendRequestValue and passIntentHeader is $passIntentHeaderFlag" in new IfsTest
            with Test {
            override def intent: Option[String] = intentValue

            val expected: Left[ResponseWrapper[RetrieveLossClaimResponse], Nothing] = Left(ResponseWrapper(correlationId, retrieveResponse))

            MockedSharedAppConfig.featureSwitchConfig returns Configuration("ifs_hip_migration_1508.enabled" -> false)
            MockedSharedAppConfig.featureSwitchConfig returns Configuration("passIntentHeader.enabled" -> passIntentHeaderFlag)

            willGet(url"$baseUrl/income-tax/claims-for-relief/$nino/$claimId")
              .returning(Future.successful(expected))

            val result: DownstreamOutcome[RetrieveLossClaimResponse] =
              await(connector.retrieveLossClaim(request = request, isAmendRequest = isAmendRequestValue))

            result shouldBe expected
          }
        }
      }
    }

    "the HIP feature switch is enabled (HIP enabled)" should {
      "return a successful response and correlationId" when {
        List(
          (false, false, None),
          (false, true, None),
          (true, false, None),
          (true, true, Some("AMEND_LOSS_CLAIM"))
        ).foreach { case (isAmendRequestValue, passIntentHeaderFlag, intentValue) =>
          s"provided with a valid request, isAmendRequest is $isAmendRequestValue and passIntentHeader is $passIntentHeaderFlag" in new HipTest
            with Test {
            override def intent: Option[String] = intentValue

            val expected: Left[ResponseWrapper[RetrieveLossClaimResponse], Nothing] = Left(ResponseWrapper(correlationId, retrieveResponse))

            MockedSharedAppConfig.featureSwitchConfig returns Configuration("ifs_hip_migration_1508.enabled" -> true)
            MockedSharedAppConfig.featureSwitchConfig returns Configuration("passIntentHeader.enabled" -> passIntentHeaderFlag)

            willGet(url"$baseUrl/itsd/income-sources/claims-for-relief/$nino/$claimId")
              .returning(Future.successful(expected))

            val result: DownstreamOutcome[RetrieveLossClaimResponse] =
              await(connector.retrieveLossClaim(request = request, isAmendRequest = isAmendRequestValue))

            result shouldBe expected
          }
        }
      }
    }
  }

  trait Test { _: ConnectorTest =>

    val connector: RetrieveLossClaimConnector =
      new RetrieveLossClaimConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)

  }

}
