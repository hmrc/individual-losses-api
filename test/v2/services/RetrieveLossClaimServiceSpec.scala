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

package v2.services

import api.models.domain.Nino
import api.models.domain.lossClaim.v2.{TypeOfClaim, TypeOfLoss}
import api.models.downstream.lossClaim.v2.LossClaimResponse
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v2.mocks.connectors.MockLossClaimConnector
import v2.models.requestData.RetrieveLossClaimRequest

import scala.concurrent.Future

class RetrieveLossClaimServiceSpec extends ServiceSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  trait Test extends MockLossClaimConnector {
    lazy val service = new RetrieveLossClaimService(connector)
  }

  lazy val request: RetrieveLossClaimRequest = RetrieveLossClaimRequest(Nino(nino), claimId)

  "retrieve loss claim" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val desResponse: ResponseWrapper[LossClaimResponse] =
          ResponseWrapper(correlationId,
                          LossClaimResponse(Some("selfEmploymentId"), TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, "2019-20", "time"))
        MockedLossClaimConnector.retrieveLossClaim(request).returns(Future.successful(Right(desResponse)))

        await(service.retrieveLossClaim(request)) shouldBe Right(desResponse)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                         = MtdError("SOME_CODE", "some message")
        val desResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedLossClaimConnector.retrieveLossClaim(request).returns(Future.successful(Left(desResponse)))

        await(service.retrieveLossClaim(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "return a downstream error" when {
      "the connector call returns a single downstream error" in new Test {
        val desResponse: ResponseWrapper[SingleError] = ResponseWrapper(correlationId, SingleError(StandardDownstreamError))
        val expected: ErrorWrapper                    = ErrorWrapper(Some(correlationId), StandardDownstreamError, None)
        MockedLossClaimConnector.retrieveLossClaim(request).returns(Future.successful(Left(desResponse)))

        await(service.retrieveLossClaim(request)) shouldBe Left(expected)
      }

      "the connector call returns multiple errors including a downstream error" in new Test {
        val desResponse: ResponseWrapper[MultipleErrors] =
          ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, StandardDownstreamError)))
        val expected: ErrorWrapper = ErrorWrapper(Some(correlationId), StandardDownstreamError, None)
        MockedLossClaimConnector.retrieveLossClaim(request).returns(Future.successful(Left(desResponse)))

        await(service.retrieveLossClaim(request)) shouldBe Left(expected)
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "SERVER_ERROR"              -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
    ).foreach {
      case (k, v) =>
        s"return a ${v.code} error" when {
          s"the connector call returns $k" in new Test {
            MockedLossClaimConnector
              .retrieveLossClaim(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "doesn't matter"))))))

            await(service.retrieveLossClaim(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }
}
