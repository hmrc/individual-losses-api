/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.services

import uk.gov.hmrc.domain.Nino
import v1.mocks.connectors.MockLossClaimConnector
import v1.models.des.LossClaimResponse
import v1.models.domain.{AmendLossClaim, TypeOfClaim, TypeOfLoss}
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData.AmendLossClaimRequest

import scala.concurrent.Future

class AmendLossClaimServiceSpec extends ServiceSpec {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino   = Nino("AA123456A")
  val claimId = "AAZZ1234567890a"

  val lossClaim = AmendLossClaim(TypeOfClaim.`carry-forward`)

  val lossClaimResponse =
    LossClaimResponse(Some("XKIS00000000988"), TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, "2019-20", "lastModified")

  val serviceUnavailableError = MtdError("SERVICE_UNAVAILABLE", "doesn't matter")

  trait Test extends MockLossClaimConnector {
    lazy val service = new AmendLossClaimService(connector)
  }

  "amend LossClaim" when {
    lazy val request = AmendLossClaimRequest(nino, claimId, lossClaim)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockedLossClaimConnector
          .amendLossClaim(request)
          .returns(Future.successful(Right(DesResponse(correlationId, lossClaimResponse))))

        await(service.amendLossClaim(request)) shouldBe Right(DesResponse(correlationId, lossClaimResponse))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError   = MtdError("SOME_CODE", "some message")
        val desResponse = DesResponse(correlationId, OutboundError(someError))
        MockedLossClaimConnector.amendLossClaim(request).returns(Future.successful(Left(desResponse)))

        await(service.amendLossClaim(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, serviceUnavailableError)))
        MockedLossClaimConnector.amendLossClaim(request).returns(Future.successful(Left(expected)))
        val result: AmendLossClaimOutcome = await(service.amendLossClaim(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
      "INVALID_PAYLOAD"           -> DownstreamError,
      "INVALID_CLAIM_TYPE"        -> RuleTypeOfClaimInvalid,
      "NOT_FOUND"                 -> NotFoundError,
      "CONFLICT"                  -> RuleClaimTypeNotChanged,
      "SERVER_ERROR"              -> DownstreamError,
      "SERVICE_UNAVAILABLE"       -> DownstreamError,
      "UNEXPECTED_ERROR"          -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            MockedLossClaimConnector
              .amendLossClaim(request)
              .returns(Future.successful(Left(DesResponse(correlationId, SingleError(MtdError(k, "MESSAGE"))))))

            await(service.amendLossClaim(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }
}
