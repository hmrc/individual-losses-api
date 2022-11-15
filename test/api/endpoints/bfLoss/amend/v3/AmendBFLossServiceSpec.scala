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

package api.endpoints.bfLoss.amend.v3

import api.endpoints.bfLoss.amend.anyVersion.request.AmendBFLossRequestBody
import api.endpoints.bfLoss.amend.v3
import api.endpoints.bfLoss.amend.v3.response.AmendBFLossResponse
import api.endpoints.bfLoss.connector.v3.MockBFLossConnector
import api.endpoints.bfLoss.domain.v3.TypeOfLoss
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._
import api.services.ServiceSpec
import api.services.v3.Outcomes.AmendBFLossOutcome

import scala.concurrent.Future

class AmendBFLossServiceSpec extends ServiceSpec {

  val nino: String                            = "AA123456A"
  val lossId: String                          = "AAZZ1234567890a"
  override implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val bfLoss: AmendBFLossRequestBody = AmendBFLossRequestBody(256.78)

  val bfLossResponse: AmendBFLossResponse = AmendBFLossResponse(
    "XKIS00000000988",
    TypeOfLoss.`self-employment`,
    256.78,
    "2019-20",
    "2018-07-13T12:13:48.763Z"
  )

  trait Test extends MockBFLossConnector {
    lazy val service = new AmendBFLossService(connector)
  }

  "amend BFLoss" when {
    lazy val request = v3.request.AmendBFLossRequest(Nino(nino), lossId, bfLoss)

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
        val someError: MtdError                                = MtdError("SOME_CODE", "some message")
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedBFLossConnector.amendBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.amendBFLoss(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "one of the errors from downstream is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: ResponseWrapper[MultipleErrors] = ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, ServiceUnavailableError)))
        MockedBFLossConnector.amendBFLoss(request).returns(Future.successful(Left(expected)))
        val result: AmendBFLossOutcome = await(service.amendBFLoss(request))
        result shouldBe Left(ErrorWrapper(correlationId, StandardDownstreamError, None))
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_LOSS_ID"           -> LossIdFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "INVALID_PAYLOAD"           -> StandardDownstreamError,
      "CONFLICT"                  -> RuleLossAmountNotChanged,
      "INVALID_CORRELATIONID"     -> StandardDownstreamError,
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

            await(service.amendBFLoss(request)) shouldBe Left(ErrorWrapper(correlationId, v, None))
          }
        }
    }
  }
}
