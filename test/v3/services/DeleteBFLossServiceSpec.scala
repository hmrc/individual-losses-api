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
import v3.models.domain.Nino
import v3.models.errors._
import v3.models.outcomes.ResponseWrapper
import v3.models.requestData.DeleteBFLossRequest

import scala.concurrent.Future

class DeleteBFLossServiceSpec extends ServiceSpec {

  val nino: String = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  trait Test extends MockBFLossConnector {
    lazy val service = new DeleteBFLossService(connector)
  }

  lazy val request: DeleteBFLossRequest = DeleteBFLossRequest(Nino(nino), lossId)

  "Delete BF Loss" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[Unit] = ResponseWrapper(correlationId, ())
        val expected: ResponseWrapper[Unit] = ResponseWrapper(correlationId, ())
        MockedBFLossConnector.deleteBFLoss(request).returns(Future.successful(Right(downstreamResponse)))

        await(service.deleteBFLoss(request)) shouldBe Right(expected)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError = MtdError("SOME_CODE", "some message")
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedBFLossConnector.deleteBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.deleteBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "return a downstream error" when {
      "the connector call returns a single downstream error" in new Test {
        val downstreamResponse: ResponseWrapper[SingleError] = ResponseWrapper(correlationId, SingleError(DownstreamError))
        val expected: ErrorWrapper = ErrorWrapper(Some(correlationId), DownstreamError, None)
        MockedBFLossConnector.deleteBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.deleteBFLoss(request)) shouldBe Left(expected)
      }
      "the connector call returns multiple errors including a downstream error" in new Test {
        val downstreamResponse: ResponseWrapper[MultipleErrors] = ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, DownstreamError)))
        val expected: ErrorWrapper = ErrorWrapper(Some(correlationId), DownstreamError, None)
        MockedBFLossConnector.deleteBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.deleteBFLoss(request)) shouldBe Left(expected)
      }
    }

    Map(
      "INVALID_IDVALUE"     -> NinoFormatError,
      "INVALID_LOSS_ID"     -> LossIdFormatError,
      "NOT_FOUND"           -> NotFoundError,
      "CONFLICT"            -> RuleDeleteAfterCrystallisationError,
      "SERVER_ERROR"        -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError,
      "UNEXPECTED_ERROR"    -> DownstreamError
    ).foreach {
      case(k, v) =>
        s"return a ${v.code} error" when {
          s"the connector call returns $k" in new Test {
            MockedBFLossConnector.deleteBFLoss(request).returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "doesn't matter"))))))

            await(service.deleteBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }
}