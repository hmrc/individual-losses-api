/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.services

import uk.gov.hmrc.domain.Nino
import v1.mocks.connectors.MockBFLossConnector
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData.DeleteBFLossRequest

import scala.concurrent.Future

class DeleteBFLossServiceSpec extends ServiceSpec {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino = Nino("AA123456A")
  val lossId = "AAZZ1234567890a"

  trait Test extends MockBFLossConnector {
    lazy val service = new DeleteBFLossService(connector)
  }

  lazy val request = DeleteBFLossRequest(nino, lossId)

  "Delete BF Loss" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val desResponse = DesResponse(correlationId, ())
        val expected = DesResponse(correlationId, ())
        MockedBFLossConnector.deleteBFLoss(request).returns(Future.successful(Right(desResponse)))

        await(service.deleteBFLoss(request)) shouldBe Right(expected)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError = MtdError("SOME_CODE", "some message")
        val desResponse = DesResponse(correlationId, OutboundError(someError))
        MockedBFLossConnector.deleteBFLoss(request).returns(Future.successful(Left(desResponse)))

        await(service.deleteBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "return a downstream error" when {
      "the connector call returns a single downstream error" in new Test {
        val desResponse = DesResponse(correlationId, SingleError(DownstreamError))
        val expected = ErrorWrapper(Some(correlationId), DownstreamError, None)
        MockedBFLossConnector.deleteBFLoss(request).returns(Future.successful(Left(desResponse)))

        await(service.deleteBFLoss(request)) shouldBe Left(expected)
      }
      "the connector call returns multiple errors including a downstream error" in new Test {
        val desResponse = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, DownstreamError)))
        val expected = ErrorWrapper(Some(correlationId), DownstreamError, None)
        MockedBFLossConnector.deleteBFLoss(request).returns(Future.successful(Left(desResponse)))

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
            MockedBFLossConnector.deleteBFLoss(request).returns(Future.successful(Left(DesResponse(correlationId, SingleError(MtdError(k, "doesn't matter"))))))

            await(service.deleteBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }

}
