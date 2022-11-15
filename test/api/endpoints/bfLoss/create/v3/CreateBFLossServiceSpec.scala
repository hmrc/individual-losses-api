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

package api.endpoints.bfLoss.create.v3

import api.endpoints.bfLoss.connector.v3.MockBFLossConnector
import api.endpoints.bfLoss.create.v3.request.{CreateBFLossRequest, CreateBFLossRequestBody}
import api.endpoints.bfLoss.create.v3.response.CreateBFLossResponse
import api.endpoints.bfLoss.domain.v3.TypeOfLoss
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._
import api.models.errors.v3.RuleDuplicateSubmissionError
import api.services.ServiceSpec
import api.services.v3.Outcomes.CreateBFLossOutcome

import scala.concurrent.Future

class CreateBFLossServiceSpec extends ServiceSpec {

  val nino: String                            = "AA123456A"
  val lossId: String                          = "AAZZ1234567890a"
  override implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val bfLoss: CreateBFLossRequestBody = CreateBFLossRequestBody(TypeOfLoss.`self-employment`, "XKIS00000000988", "2019-20", 256.78)

  trait Test extends MockBFLossConnector {
    lazy val service = new CreateBFLossService(connector)
  }

  "create BFLoss" when {
    lazy val request = CreateBFLossRequest(Nino(nino), bfLoss)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockedBFLossConnector
          .createBFLoss(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, CreateBFLossResponse(lossId)))))

        await(service.createBFLoss(request)) shouldBe Right(ResponseWrapper(correlationId, CreateBFLossResponse(lossId)))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedBFLossConnector.createBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.createBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "one of the errors from downstream is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: ResponseWrapper[MultipleErrors] = ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, ServiceUnavailableError)))
        MockedBFLossConnector.createBFLoss(request).returns(Future.successful(Left(expected)))
        val result: CreateBFLossOutcome = await(service.createBFLoss(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), StandardDownstreamError, None))
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "DUPLICATE_SUBMISSION"      -> RuleDuplicateSubmissionError,
      "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError,
      "TAX_YEAR_NOT_ENDED"        -> RuleTaxYearNotEndedError,
      "INCOME_SOURCE_NOT_FOUND"   -> NotFoundError,
      "INVALID_CORRELATIONID"     -> StandardDownstreamError,
      "INVALID_PAYLOAD"           -> StandardDownstreamError,
      "SERVER_ERROR"              -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            MockedBFLossConnector
              .createBFLoss(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "MESSAGE", v.httpStatus))))))

            await(service.createBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }
}
