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

package v4.services

import common.errors.RuleDuplicateSubmissionError
import shared.models.domain.Nino
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v4.connectors.MockCreateBFLossConnector
import v4.models.domain.bfLoss.TypeOfLoss
import v4.models.request.createBFLosses.{CreateBFLossRequestBody, CreateBFLossRequestData}
import v4.models.response.createBFLosses.CreateBFLossResponse

import scala.concurrent.Future

class CreateBFLossServiceSpec extends ServiceSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val bfLoss: CreateBFLossRequestBody = CreateBFLossRequestBody(TypeOfLoss.`self-employment`, "XKIS00000000988", "2019-20", 256.78)

  trait Test extends MockCreateBFLossConnector {
    lazy val service = new CreateBFLossService(connector)
  }

  "create BFLoss" when {
    lazy val request = CreateBFLossRequestData(Nino(nino), bfLoss)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockCreateBFLossConnector
          .createBFLoss(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, CreateBFLossResponse(lossId)))))

        await(service.createBFLoss(request)) shouldBe Right(ResponseWrapper(correlationId, CreateBFLossResponse(lossId)))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockCreateBFLossConnector.createBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.createBFLoss(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockCreateBFLossConnector
            .createBFLoss(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.createBFLoss(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors: Seq[(String, MtdError)] = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "DUPLICATE_SUBMISSION"      -> RuleDuplicateSubmissionError,
        "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError,
        "TAX_YEAR_NOT_ENDED"        -> RuleTaxYearNotEndedError,
        "INCOME_SOURCE_NOT_FOUND"   -> NotFoundError,
        "INVALID_CORRELATIONID"     -> InternalError,
        "INVALID_PAYLOAD"           -> InternalError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError
      )

      errors.foreach(args => (serviceError _).tupled(args))
    }

  }

}
