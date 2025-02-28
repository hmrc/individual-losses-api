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

package v6.bfLosses.amend

import shared.connectors.{ConnectorSpec, DownstreamOutcome}
import shared.models.domain.{Nino, TaxYear, Timestamp}
import shared.models.outcomes.ResponseWrapper
import v6.bfLosses.amend.def1.model.request.{Def1_AmendBFLossRequestBody, Def1_AmendBFLossRequestData}
import v6.bfLosses.amend.def1.model.response.Def1_AmendBFLossResponse
import v6.bfLosses.amend.model.response.AmendBFLossResponse
import v6.bfLosses.common.domain.{LossId, TypeOfLoss}

import scala.concurrent.Future

class AmendBFLossConnectorSpec extends ConnectorSpec {

  val nino: String                    = "AA123456A"
  val lossId: String                  = "AAZZ1234567890a"
  val taxYear: String                 = "2019-20"
  val taxYearDownstreamFormat: String = "19-20"

  val requestBody: Def1_AmendBFLossRequestBody = Def1_AmendBFLossRequestBody(500.13)

  val request: Def1_AmendBFLossRequestData =
    Def1_AmendBFLossRequestData(nino = Nino(nino), lossId = LossId(lossId), taxYear = TaxYear.fromMtd(taxYear), requestBody)

  "amendBFLosses" should {
    "return the expected response for a non-TYS request" when {
      "downstream returns OK" in new IfsTest with Test {
        val response: AmendBFLossResponse = Def1_AmendBFLossResponse(
          businessId = "XKIS00000000988",
          typeOfLoss = TypeOfLoss.`self-employment`,
          lossAmount = 500.13,
          taxYearBroughtForwardFrom = "2019-20",
          lastModified = Timestamp("2018-07-13T12:13:48.763Z")
        )
        val expected: Right[Nothing, ResponseWrapper[AmendBFLossResponse]] = Right(ResponseWrapper(correlationId, response))

        willPut(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$taxYearDownstreamFormat/$lossId",
          body = requestBody
        ).returning(Future.successful(expected))

        val result: DownstreamOutcome[AmendBFLossResponse] = await(connector.amendBFLoss(request))
        result shouldBe expected
      }
    }
  }

  trait Test { _: ConnectorTest =>
    val connector: AmendBFLossConnector = new AmendBFLossConnector(http = mockHttpClient, appConfig = mockSharedAppConfig)
  }

}
