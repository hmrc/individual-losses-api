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

import common.errors.LossIdFormatError
import shared.models.domain.{Nino, Timestamp}
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v4.connectors.MockRetrieveBFLossConnector
import v4.models.domain.bfLoss.{LossId, TypeOfLoss}
import v4.models.request.retrieveBFLoss.RetrieveBFLossRequestData
import v4.models.response.retrieveBFLoss.RetrieveBFLossResponse

import scala.concurrent.Future

class RetrieveBFLossServiceSpec extends ServiceSpec {

  val nino: String = "AA123456A"
  val lossId       = "AAZZ1234567890a"

  trait Test extends MockRetrieveBFLossConnector {
    lazy val service = new RetrieveBFLossService(connector)
  }

  lazy val request: RetrieveBFLossRequestData = RetrieveBFLossRequestData(Nino(nino), LossId(lossId))

  "retrieve bf loss" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[RetrieveBFLossResponse] =
          ResponseWrapper(
            correlationId,
            RetrieveBFLossResponse("selfEmploymentId", TypeOfLoss.`self-employment`, 123.45, "2018-19", Timestamp("2018-07-13T12:13:48.763Z")))
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

      val ifsErrors: Seq[(String, MtdError)] = Seq(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_LOSS_ID"           -> LossIdFormatError,
        "NOT_FOUND"                 -> NotFoundError,
        "SERVER_ERROR"              -> InternalError,
        "INVALID_CORRELATIONID"     -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError
      )

      val hipErrors = Map(
        "1215" -> NinoFormatError,
        "1219" -> LossIdFormatError,
        "5010" -> NotFoundError
      )

      (ifsErrors ++ hipErrors).foreach(args => (serviceError _).tupled(args))
    }
  }

}
