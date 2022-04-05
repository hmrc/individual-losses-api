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
import api.models.domain.v2.{ AmendLossClaim, TypeOfClaim, TypeOfLoss }
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v2.mocks.connectors.MockLossClaimConnector
import v2.models.des.LossClaimResponse
import v2.models.errors._
import v2.models.requestData.AmendLossClaimRequest

import scala.concurrent.Future

class AmendLossClaimServiceSpec extends ServiceSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  val lossClaim: AmendLossClaim = AmendLossClaim(TypeOfClaim.`carry-forward`)

  val lossClaimResponse: LossClaimResponse =
    LossClaimResponse(Some("XKIS00000000988"), TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, "2019-20", "lastModified")

  val serviceUnavailableError: MtdError = MtdError("SERVICE_UNAVAILABLE", "doesn't matter")

  trait Test extends MockLossClaimConnector {
    lazy val service = new AmendLossClaimService(connector)
  }

  "amend LossClaim" when {
    lazy val request = AmendLossClaimRequest(Nino(nino), claimId, lossClaim)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockedLossClaimConnector
          .amendLossClaim(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, lossClaimResponse))))

        await(service.amendLossClaim(request)) shouldBe Right(ResponseWrapper(correlationId, lossClaimResponse))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                         = MtdError("SOME_CODE", "some message")
        val desResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedLossClaimConnector.amendLossClaim(request).returns(Future.successful(Left(desResponse)))

        await(service.amendLossClaim(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: ResponseWrapper[MultipleErrors] = ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, serviceUnavailableError)))
        MockedLossClaimConnector.amendLossClaim(request).returns(Future.successful(Left(expected)))
        val result: AmendLossClaimOutcome = await(service.amendLossClaim(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), StandardDownstreamError, None))
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
      "INVALID_PAYLOAD"           -> StandardDownstreamError,
      "INVALID_CLAIM_TYPE"        -> RuleTypeOfClaimInvalid,
      "NOT_FOUND"                 -> NotFoundError,
      "CONFLICT"                  -> RuleClaimTypeNotChanged,
      "SERVER_ERROR"              -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"       -> StandardDownstreamError,
      "UNEXPECTED_ERROR"          -> StandardDownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            MockedLossClaimConnector
              .amendLossClaim(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "MESSAGE"))))))

            await(service.amendLossClaim(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }
}
