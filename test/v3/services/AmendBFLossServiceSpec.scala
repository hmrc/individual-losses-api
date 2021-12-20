/*
 * Copyright 2021 HM Revenue & Customs
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

package v3.services

import v3.mocks.connectors.MockBFLossConnector
import v3.models.downstream.BFLossResponse
import v3.models.domain.{AmendBFLoss, Nino, TypeOfLoss}
import v3.models.errors._
import v3.models.outcomes.ResponseWrapper
import v3.models.requestData.AmendBFLossRequest

import scala.concurrent.Future

class AmendBFLossServiceSpec extends ServiceSpec {

  val nino: String = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val bfLoss: AmendBFLoss = AmendBFLoss(256.78)

  val bfLossResponse: BFLossResponse = BFLossResponse(
    "XKIS00000000988",
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
        val someError: MtdError = MtdError("SOME_CODE", "some message")
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedBFLossConnector.amendBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.amendBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "one of the errors from downstream is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: ResponseWrapper[MultipleErrors] = ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, serviceUnavailableError)))
        MockedBFLossConnector.amendBFLoss(request).returns(Future.successful(Left(expected)))
        val result: AmendBFLossOutcome = await(service.amendBFLoss(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID"  -> NinoFormatError,
      "INVALID_LOSS_ID"            -> LossIdFormatError,
      "NOT_FOUND"                  -> NotFoundError,
      "INVALID_PAYLOAD"            -> DownstreamError,
      "CONFLICT"                   -> RuleLossAmountNotChanged,
      "SERVER_ERROR"               -> DownstreamError,
      "SERVICE_UNAVAILABLE"        -> DownstreamError,
      "UNEXPECTED_ERROR"           -> DownstreamError
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