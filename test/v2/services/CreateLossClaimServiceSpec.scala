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

import api.models.errors._
import api.models.outcomes.ResponseWrapper
import v2.mocks.connectors.MockLossClaimConnector
import v2.models.des.CreateLossClaimResponse
import v2.models.domain.{LossClaim, Nino, TypeOfClaim, TypeOfLoss}
import v2.models.errors._
import v2.models.requestData.CreateLossClaimRequest

import scala.concurrent.Future

class CreateLossClaimServiceSpec extends ServiceSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  val lossClaim: LossClaim = LossClaim("2018", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, Some("XKIS00000000988"))

  val serviceUnavailableError: MtdError = MtdError("SERVICE_UNAVAILABLE", "doesn't matter")

  trait Test extends MockLossClaimConnector {
    lazy val service = new CreateLossClaimService(connector)
  }

  "create LossClaim" when {
    lazy val request = CreateLossClaimRequest(Nino(nino), lossClaim)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockedLossClaimConnector
          .createLossClaim(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, CreateLossClaimResponse(claimId)))))

        await(service.createLossClaim(request)) shouldBe Right(ResponseWrapper(correlationId, CreateLossClaimResponse(claimId)))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                         = MtdError("SOME_CODE", "some message")
        val desResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedLossClaimConnector.createLossClaim(request).returns(Future.successful(Left(desResponse)))

        await(service.createLossClaim(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: ResponseWrapper[MultipleErrors] = ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, serviceUnavailableError)))
        MockedLossClaimConnector.createLossClaim(request).returns(Future.successful(Left(expected)))
        val result: CreateLossClaimOutcome = await(service.createLossClaim(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), StandardDownstreamError, None))
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID"   -> NinoFormatError,
      "DUPLICATE"                   -> RuleDuplicateClaimSubmissionError,
      "ACCOUNTING_PERIOD_NOT_ENDED" -> RulePeriodNotEnded,
      "INVALID_CLAIM_TYPE"          -> RuleTypeOfClaimInvalid,
      "NOT_FOUND_INCOME_SOURCE"     -> NotFoundError,
      "TAX_YEAR_NOT_SUPPORTED"      -> RuleTaxYearNotSupportedError,
      "NO_ACCOUNTING_PERIOD"        -> RuleNoAccountingPeriod,
      "INVALID_PAYLOAD"             -> StandardDownstreamError,
      "SERVER_ERROR"                -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"         -> StandardDownstreamError,
      "UNEXPECTED_ERROR"            -> StandardDownstreamError,
      "INCOMESOURCE_ID_REQUIRED"    -> RuleBusinessId,
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            MockedLossClaimConnector
              .createLossClaim(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "MESSAGE"))))))

            await(service.createLossClaim(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }
}
