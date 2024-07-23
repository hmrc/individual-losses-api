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

package v5.bfLosses.delete

import api.connectors.ConnectorSpec
import api.models.domain.Nino
import api.models.outcomes.ResponseWrapper
import v5.bfLosses.common.domain.LossId
import v5.bfLosses.delete.DeleteBFLossConnector
import v5.bfLosses.delete.def1.model.request.Def1_DeleteBFLossRequestData
import v5.bfLosses.delete.model.request.DeleteBFLossRequestData

import scala.concurrent.Future

class DeleteBFLossConnectorSpec extends ConnectorSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val request: DeleteBFLossRequestData = Def1_DeleteBFLossRequestData(nino = Nino(nino), lossId = LossId(lossId))

  trait Test {
    _: ConnectorTest =>

    val connector: DeleteBFLossConnector = new DeleteBFLossConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

  "deleteBFLosses" should {
    "return the expected response for a non-TYS request" when {
      "downstream returns OK" in new DesTest with Test {
        val expected: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        willDelete(
          url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId"
        ).returns(Future.successful(expected))

        await(connector.deleteBFLoss(request)) shouldBe expected
      }
    }
  }

}
