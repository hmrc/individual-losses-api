/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.bfLosses.delete

import common.errors.{LossIdFormatError, RuleDeleteAfterFinalDeclarationError, RuleOutsideAmendmentWindow}
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v6.bfLosses.common.domain.LossId
import v6.bfLosses.delete
import v6.bfLosses.delete.def1.model.request.Def1_DeleteBFLossRequestData

import scala.concurrent.Future

class DeleteBFLossServiceSpec extends ServiceSpec {

  private val nino: String    = "AA123456A"
  private val lossId: String  = "AAZZ1234567890a"
  private val taxYear: String = "2019-20"

  trait Test extends delete.MockDeleteBFLossConnector {
    lazy val service = new DeleteBFLossService(connector)
  }

  lazy val request: Def1_DeleteBFLossRequestData = Def1_DeleteBFLossRequestData(Nino(nino), LossId(lossId), TaxYear.fromMtd(taxYear))

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
        "INVALID_TAX_YEAR"          -> TaxYearFormatError,
        "NOT_FOUND"                 -> NotFoundError,
        "CONFLICT"                  -> RuleDeleteAfterFinalDeclarationError,
        "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError,
        "UNEXPECTED_ERROR"          -> InternalError
      )

      errors.foreach(args => (serviceError _).tupled(args))
    }

  }

}
