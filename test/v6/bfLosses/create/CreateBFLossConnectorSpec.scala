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

package v6.bfLosses.create

import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.Nino
import shared.models.outcomes.ResponseWrapper
import v6.bfLosses.common.domain.TypeOfLoss
import v6.bfLosses.create.def1.model.request.{Def1_CreateBFLossRequestBody, Def1_CreateBFLossRequestData}
import v6.bfLosses.create.def1.model.response.Def1_CreateBFLossResponse
import v6.bfLosses.create.model.response.CreateBFLossResponse

import scala.concurrent.Future

class CreateBFLossConnectorSpec extends ConnectorSpec {

  val nino   = "AA123456A"
  val lossId = "AAZZ1234567890a"

  private val requestBody = Def1_CreateBFLossRequestBody(TypeOfLoss.`self-employment`, "XKIS00000000988", "2019-20", 256.78)
  private val request     = Def1_CreateBFLossRequestData(nino = Nino(nino), requestBody)

  "createBFLosses" should {
    "return the expected response for a non-TYS request" when {
      "downstream returns OK" in new IfsTest with Test {
        val response: CreateBFLossResponse                                  = Def1_CreateBFLossResponse(lossId)
        val expected: Right[Nothing, ResponseWrapper[CreateBFLossResponse]] = Right(ResponseWrapper(correlationId, response))

        willPost(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
          body = requestBody
        ).returning(Future.successful(expected))

        val result: DownstreamOutcome[CreateBFLossResponse] = await(connector.createBFLoss(request))
        result shouldBe expected
      }
    }
  }

  trait Test { _: ConnectorTest =>
    val connector: CreateBFLossConnector = new CreateBFLossConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)
  }

}
