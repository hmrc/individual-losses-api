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

package v3.connectors

import api.connectors.ConnectorSpec
import api.models.domain.Nino
import api.models.outcomes.ResponseWrapper
import v3.models.domain.bfLoss.TypeOfLoss
import v3.models.request.createBFLosses.{CreateBFLossRequestBody, CreateBFLossRequestData}
import v3.models.response.createBFLosses.CreateBFLossResponse

import scala.concurrent.Future

class CreateBFLossConnectorSpec extends ConnectorSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val requestBody: CreateBFLossRequestBody = CreateBFLossRequestBody(TypeOfLoss.`self-employment`, "XKIS00000000988", "2019-20", 256.78)
  val request: CreateBFLossRequestData         = CreateBFLossRequestData(nino = Nino(nino), requestBody)

  trait Test {
    _: ConnectorTest =>

    val connector: CreateBFLossConnector = new CreateBFLossConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

  "createBFLosses" should {
    "return the expected response for a non-TYS request" when {
      "downstream returns OK" in new IfsTest with Test {
        val response: CreateBFLossResponse                                  = CreateBFLossResponse(lossId)
        val expected: Right[Nothing, ResponseWrapper[CreateBFLossResponse]] = Right(ResponseWrapper(correlationId, response))

        willPost(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
          body = requestBody
        ).returns(Future.successful(expected))

        await(connector.createBFLoss(request)) shouldBe expected
      }
    }
  }

}
