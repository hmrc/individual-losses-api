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

package v5.bfLosses.amend

import api.models.domain.{Nino, Timestamp}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v5.bfLosses.amend
import v5.bfLosses.amend.AmendBFLossService
import v5.bfLosses.amend.def1.model.request.{Def1_AmendBFLossRequestBody, Def1_AmendBFLossRequestData}
import v5.bfLosses.amend.def1.model.response.Def1_AmendBFLossResponse
import v5.bfLosses.domain._

import scala.concurrent.Future

class AmendBFLossServiceSpec extends ServiceSpec {

  private val nino   = Nino("AA123456A")
  private val lossId = LossId("AAZZ1234567890a")

  val requestData: Def1_AmendBFLossRequestData = Def1_AmendBFLossRequestData(nino, lossId, Def1_AmendBFLossRequestBody(256.78))

  val bfLossResponse: Def1_AmendBFLossResponse = Def1_AmendBFLossResponse(
    "XKIS00000000988",
    TypeOfLoss.`self-employment`,
    256.78,
    "2019-20",
    Timestamp("2018-07-13T12:13:48.763Z")
  )

  "amend BFLoss" when {

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockAmendBFLossConnector
          .amendBFLoss(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, bfLossResponse))))

        await(service.amendBFLoss(requestData)) shouldBe Right(ResponseWrapper(correlationId, bfLossResponse))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockAmendBFLossConnector.amendBFLoss(requestData).returns(Future.successful(Left(downstreamResponse)))

        await(service.amendBFLoss(requestData)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    def serviceError(downstreamErrorCode: String, error: MtdError): Unit = {
      s"a $downstreamErrorCode error is returned from the service" in new Test {
        MockAmendBFLossConnector
          .amendBFLoss(requestData)
          .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

        await(service.amendBFLoss(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
      }
    }

    val errors = List(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_LOSS_ID"           -> LossIdFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "INVALID_PAYLOAD"           -> InternalError,
      "CONFLICT"                  -> RuleLossAmountNotChanged,
      "INVALID_CORRELATIONID"     -> InternalError,
      "SERVER_ERROR"              -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError,
      "UNEXPECTED_ERROR"          -> InternalError
    )

    errors.foreach(args => (serviceError _).tupled(args))
  }

  trait Test extends amend.MockAmendBFLossConnector {
    val service = new AmendBFLossService(connector)
  }

}
