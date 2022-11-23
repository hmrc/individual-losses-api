/*
 * Copyright 2022 HM Revenue & Customs
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

package api.endpoints.bfLoss.retrieve.v3.connector

import api.connectors.ConnectorSpec
import api.endpoints.bfLoss.connector.v3.BFLossConnector
import api.endpoints.bfLoss.domain.v3.TypeOfLoss
import api.models.errors.{ LossIdFormatError, MultipleErrors, NinoFormatError, SingleError }
import api.endpoints.bfLoss.retrieve.v3.request.RetrieveBFLossRequest
import api.endpoints.bfLoss.retrieve.v3.response.RetrieveBFLossResponse
import api.models.ResponseWrapper
import api.models.domain.Nino

import scala.concurrent.Future

class RetrieveBFLossConnectorSpec extends ConnectorSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val request: RetrieveBFLossRequest = RetrieveBFLossRequest(nino = Nino(nino), lossId = lossId)

  trait Test {
    _: ConnectorTest =>

    val connector: BFLossConnector = new BFLossConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

  "retrieveBFLosses" when {
    "return a successful response with the correct correlationId" should {
      val response: RetrieveBFLossResponse = RetrieveBFLossResponse(
        businessId = "fakeId",
        typeOfLoss = TypeOfLoss.`self-employment`,
        lossAmount = 2000.25,
        taxYearBroughtForwardFrom = "2018-19",
        lastModified = "dateString"
      )

      "a valid non-TYS request is supplied" in new IfsTest with Test {
        val expected = Right(ResponseWrapper(correlationId, response))

        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
        ).returns(Future.successful(expected))

        await(connector.retrieveBFLoss(request)) shouldBe expected
      }
    }

    "return an unsuccessful response" should {
      "a non-valid request with a single error be supplied" in new IfsTest with Test {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
        ).returns(Future.successful(expected))

        await(connector.retrieveBFLoss(request)) shouldBe expected
      }

      "a non-valid request with multiple errors be supplied" in new IfsTest with Test {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, LossIdFormatError))))

        willGet(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
        ).returns(Future.successful(expected))

        await(connector.retrieveBFLoss(request)) shouldBe expected
      }
    }
  }
}