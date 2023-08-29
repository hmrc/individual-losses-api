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
import api.models.ResponseWrapper
import api.models.domain.{Nino, Timestamp}
import v3.models.domain.bfLoss.TypeOfLoss
import v3.models.request.amendBFLosses.{AmendBFLossRequest, AmendBFLossRequestBody}
import v3.models.response.amendBFLosses.AmendBFLossResponse

import scala.concurrent.Future

class AmendBFLossConnectorSpec extends ConnectorSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val requestBody: AmendBFLossRequestBody = AmendBFLossRequestBody(500.13)
  val request: AmendBFLossRequest         = AmendBFLossRequest(nino = Nino(nino), lossId = lossId, requestBody)

  trait Test {
    _: ConnectorTest =>

    val connector: AmendBFLossConnector = new AmendBFLossConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

  "amendBFLosses" should {
    "return the expected response for a non-TYS request" when {
      "downstream returns OK" in new IfsTest with Test {
        val response: AmendBFLossResponse = AmendBFLossResponse(
          businessId = "XKIS00000000988",
          typeOfLoss = TypeOfLoss.`self-employment`,
          lossAmount = 500.13,
          taxYearBroughtForwardFrom = "2019-20",
          lastModified = Timestamp("2018-07-13T12:13:48.763Z")
        )
        val expected: Right[Nothing, ResponseWrapper[AmendBFLossResponse]] = Right(ResponseWrapper(correlationId, response))

        willPut(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
          body = requestBody
        ).returns(Future.successful(expected))

        await(connector.amendBFLoss(request)) shouldBe expected
      }
    }
  }

}
