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
import shared.models.domain.{Nino, Timestamp}
import shared.models.outcomes.ResponseWrapper
import v4.models.domain.bfLoss.{LossId, TypeOfLoss}
import v4.models.request.retrieveBFLoss.RetrieveBFLossRequestData
import v4.models.response.retrieveBFLoss.RetrieveBFLossResponse

import scala.concurrent.Future

class RetrieveBFLossConnectorSpec extends ConnectorSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val request: RetrieveBFLossRequestData = RetrieveBFLossRequestData(nino = Nino(nino), lossId = LossId(lossId))

  trait Test {
    _: ConnectorTest =>

    val connector: RetrieveBFLossConnector = new RetrieveBFLossConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)

  }

  "retrieveBFLosses" should {
    "return the expected response for a non-TYS request" when {
      "downstream returns OK" in new IfsTest with Test {
        val response: RetrieveBFLossResponse = RetrieveBFLossResponse(
          businessId = "fakeId",
          typeOfLoss = TypeOfLoss.`self-employment`,
          lossAmount = 2000.25,
          taxYearBroughtForwardFrom = "2018-19",
          lastModified = Timestamp("2018-07-13T12:13:48.763Z")
        )
        val expected: Right[Nothing, ResponseWrapper[RetrieveBFLossResponse]] = Right(ResponseWrapper(correlationId, response))

        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId"
        ).returning(Future.successful(expected))

        val result: DownstreamOutcome[RetrieveBFLossResponse] = await(connector.retrieveBFLoss(request))
        result shouldBe expected
      }
    }
  }

}
