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

package api.endpoints.bfLoss.retrieve.v3

import api.endpoints.bfLoss.connector.v3.MockBFLossConnector
import api.endpoints.bfLoss.domain.v3.TypeOfLoss
import api.endpoints.bfLoss.retrieve.v3.request.RetrieveBFLossRequest
import api.endpoints.bfLoss.retrieve.v3.response.RetrieveBFLossResponse
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._
import api.services.ServiceSpec

import scala.concurrent.Future

class RetrieveBFLossServiceSpec extends ServiceSpec {

  val nino: String                            = "AA123456A"
  val lossId                                  = "AAZZ1234567890a"
  override implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  trait Test extends MockBFLossConnector {
    lazy val service = new RetrieveBFLossService(connector)
  }

  lazy val request: RetrieveBFLossRequest = RetrieveBFLossRequest(Nino(nino), lossId)

  "retrieve bf loss" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[RetrieveBFLossResponse] =
          ResponseWrapper(correlationId, RetrieveBFLossResponse("selfEmploymentId", TypeOfLoss.`self-employment`, 123.45, "2018-19", "time"))
        MockedBFLossConnector.retrieveBFLoss(request).returns(Future.successful(Right(downstreamResponse)))

        await(service.retrieveBFLoss(request)) shouldBe Right(downstreamResponse)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message")
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedBFLossConnector.retrieveBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.retrieveBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "return a downstream error" when {
      "the connector call returns a single downstream error" in new Test {
        val downstreamResponse: ResponseWrapper[SingleError] = ResponseWrapper(correlationId, SingleError(StandardDownstreamError))
        val expected: ErrorWrapper                           = ErrorWrapper(Some(correlationId), StandardDownstreamError, None)
        MockedBFLossConnector.retrieveBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.retrieveBFLoss(request)) shouldBe Left(expected)
      }

      "the connector call returns multiple errors including a downstream error" in new Test {
        val downstreamResponse: ResponseWrapper[MultipleErrors] =
          ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, StandardDownstreamError)))
        val expected: ErrorWrapper = ErrorWrapper(Some(correlationId), StandardDownstreamError, None)
        MockedBFLossConnector.retrieveBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.retrieveBFLoss(request)) shouldBe Left(expected)
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_LOSS_ID"           -> LossIdFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "SERVER_ERROR"              -> StandardDownstreamError,
      "INVALID_CORRELATIONID"     -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
    ).foreach {
      case (k, v) =>
        s"return a ${v.code} error" when {
          s"the connector call returns $k" in new Test {
            MockedBFLossConnector
              .retrieveBFLoss(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "doesn't matter", v.httpStatus))))))

            await(service.retrieveBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }
}
