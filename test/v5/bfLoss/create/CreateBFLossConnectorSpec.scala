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

package v5.bfLoss.create

import api.connectors.ConnectorSpec
import api.models.domain.Nino
import api.models.outcomes.ResponseWrapper
import v5.bfLossClaims.create.CreateBFLossConnector
import v5.bfLossClaims.create.def1.model.request.{Def1_CreateBFLossRequestBody, Def1_CreateBFLossRequestData}
import v5.bfLossClaims.create.def1.model.response.Def1_CreateBFLossResponse
import v5.bfLossClaims.create.model._
import v5.bfLossClaims.create.model.request.CreateBFLossRequestData
import v5.bfLossClaims.create.model.response.CreateBFLossResponse

import scala.concurrent.Future

class CreateBFLossConnectorSpec extends ConnectorSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val requestBody: Def1_CreateBFLossRequestBody = Def1_CreateBFLossRequestBody(TypeOfLoss.`self-employment`, "XKIS00000000988", "2019-20", 256.78)
  val request: CreateBFLossRequestData         = Def1_CreateBFLossRequestData(nino = Nino(nino), requestBody)

  trait Test {
    _: ConnectorTest =>

    val connector: CreateBFLossConnector = new CreateBFLossConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

  "createBFLosses" should {
    "return the expected response for a non-TYS request" when {
      "downstream returns OK" in new IfsTest with Test {
        val response: CreateBFLossResponse                                  = Def1_CreateBFLossResponse(lossId)
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
