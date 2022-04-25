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

package api.endpoints.amendBFLoss.v2

import api.connectors.v2.MockBFLossConnector
import api.endpoints.amendBFLoss.common.request.AmendBFLossRequestBody
import api.endpoints.amendBFLoss.v2.request.AmendBFLossRequest
import api.endpoints.common.lossClaim.v2.domain.TypeOfLoss
import api.models.domain.Nino
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v2.models.des.BFLossResponse
import v2.models.errors._

import scala.concurrent.Future

class AmendBFLossServiceSpec extends ServiceSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val bfLoss: AmendBFLossRequestBody = AmendBFLossRequestBody(256.78)

  val bfLossResponse: BFLossResponse = BFLossResponse(
    Some("XKIS00000000988"),
    TypeOfLoss.`self-employment`,
    256.78,
    "2019-20",
    "2018-07-13T12:13:48.763Z"
  )

  val serviceUnavailableError: MtdError = MtdError("SERVICE_UNAVAILABLE", "doesn't matter")

  trait Test extends MockBFLossConnector {
    lazy val service = new AmendBFLossService(connector)
  }

  "amend BFLoss" when {
    lazy val request = AmendBFLossRequest(Nino(nino), lossId, bfLoss)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockedBFLossConnector
          .amendBFLoss(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, bfLossResponse))))

        await(service.amendBFLoss(request)) shouldBe Right(ResponseWrapper(correlationId, bfLossResponse))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                         = MtdError("SOME_CODE", "some message")
        val desResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedBFLossConnector.amendBFLoss(request).returns(Future.successful(Left(desResponse)))

        await(service.amendBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: ResponseWrapper[MultipleErrors] = ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, serviceUnavailableError)))
        MockedBFLossConnector.amendBFLoss(request).returns(Future.successful(Left(expected)))
        val result: AmendBFLossOutcome = await(service.amendBFLoss(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), StandardDownstreamError, None))
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_LOSS_ID"           -> LossIdFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "INVALID_PAYLOAD"           -> StandardDownstreamError,
      "CONFLICT"                  -> RuleLossAmountNotChanged,
      "SERVER_ERROR"              -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"       -> StandardDownstreamError,
      "UNEXPECTED_ERROR"          -> StandardDownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            MockedBFLossConnector
              .amendBFLoss(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "MESSAGE"))))))

            await(service.amendBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }
}
