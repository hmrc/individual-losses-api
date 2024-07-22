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

package v5.bfLoss.retrieve

import api.models.domain.{Nino, Timestamp}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v5.bfLoss.retrieve
import v5.bfLosses.retrieve.RetrieveBFLossService
import v5.bfLosses.retrieve.def1.model.request.Def1_RetrieveBFLossRequestData
import v5.bfLosses.retrieve.def1.model.response.Def1_RetrieveBFLossResponse
import v5.bfLosses.retrieve.model._

import scala.concurrent.Future

class RetrieveBFLossServiceSpec extends ServiceSpec {

  val nino: String = "AA123456A"
  val lossId       = "AAZZ1234567890a"

  trait Test extends retrieve.MockRetrieveBFLossConnector {
    lazy val service = new RetrieveBFLossService(connector)
  }

  lazy val request: Def1_RetrieveBFLossRequestData = Def1_RetrieveBFLossRequestData(Nino(nino), LossId(lossId))

  "retrieve bf loss" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[Def1_RetrieveBFLossResponse] =
          ResponseWrapper(
            correlationId,
            Def1_RetrieveBFLossResponse("selfEmploymentId", TypeOfLoss.`self-employment`, 123.45, "2018-19", Timestamp("2018-07-13T12:13:48.763Z")))
        MockRetrieveBFLossConnector.retrieveBFLoss(request).returns(Future.successful(Right(downstreamResponse)))

        await(service.retrieveBFLoss(request)) shouldBe Right(downstreamResponse)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockRetrieveBFLossConnector.retrieveBFLoss(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.retrieveBFLoss(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockRetrieveBFLossConnector
            .retrieveBFLoss(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.retrieveBFLoss(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors: Seq[(String, MtdError)] = Seq(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_LOSS_ID"           -> LossIdFormatError,
        "NOT_FOUND"                 -> NotFoundError,
        "SERVER_ERROR"              -> InternalError,
        "INVALID_CORRELATIONID"     -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError
      )

      errors.foreach(args => (serviceError _).tupled(args))
    }
  }

}
