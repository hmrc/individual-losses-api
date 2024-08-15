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

import shared.models.domain.Nino
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v4.connectors.MockDeleteBFLossConnector
import v4.models.domain.bfLoss.LossId
import v4.models.request.deleteBFLosses.DeleteBFLossRequestData

import scala.concurrent.Future

class DeleteBFLossServiceSpec extends ServiceSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  trait Test extends MockDeleteBFLossConnector {
    lazy val service = new DeleteBFLossService(connector)
  }

  lazy val request: DeleteBFLossRequestData = DeleteBFLossRequestData(Nino(nino), LossId(lossId))

  "Delete BF Loss" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[Unit] = ResponseWrapper(correlationId, ())
        val expected: ResponseWrapper[Unit]           = ResponseWrapper(correlationId, ())
        MockDeleteBFLossConnector.deleteBFLoss(request).returns(Future.successful(Right(downstreamResponse)))

        await(service.deleteBFLoss(request)) shouldBe Right(expected)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockDeleteBFLossConnector.deleteBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.deleteBFLoss(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockDeleteBFLossConnector
            .deleteBFLoss(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.deleteBFLoss(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors: Seq[(String, MtdError)] = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_LOSS_ID"           -> LossIdFormatError,
        "NOT_FOUND"                 -> NotFoundError,
        "CONFLICT"                  -> RuleDeleteAfterFinalDeclarationError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError,
        "UNEXPECTED_ERROR"          -> InternalError
      )

      errors.foreach(args => (serviceError _).tupled(args))
    }

  }

}
