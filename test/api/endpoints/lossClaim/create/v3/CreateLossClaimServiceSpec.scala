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

package api.endpoints.lossClaim.create.v3

import api.endpoints.lossClaim.connector.v3.MockLossClaimConnector
import api.endpoints.lossClaim.create.v3.request.{CreateLossClaimRequest, CreateLossClaimRequestBody}
import api.endpoints.lossClaim.create.v3.response.CreateLossClaimResponse
import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._
import api.models.errors.v3.{RuleDuplicateClaimSubmissionError, RuleNoAccountingPeriod, RulePeriodNotEnded, RuleTypeOfClaimInvalid}
import api.services.ServiceSpec
import api.services.v3.Outcomes.CreateLossClaimOutcome

import scala.concurrent.Future

class CreateLossClaimServiceSpec extends ServiceSpec {

  val nino: String                            = "AA123456A"
  val claimId: String                         = "AAZZ1234567890a"
  override implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val lossClaim: CreateLossClaimRequestBody =
    CreateLossClaimRequestBody("2018", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, "XKIS00000000988")

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
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedLossClaimConnector.createLossClaim(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.createLossClaim(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "one of the errors from downstream is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: ResponseWrapper[MultipleErrors] = ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, ServiceUnavailableError)))
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
      "INCOME_SOURCE_NOT_FOUND"     -> NotFoundError,
      "TAX_YEAR_NOT_SUPPORTED"      -> RuleTaxYearNotSupportedError,
      "NO_ACCOUNTING_PERIOD"        -> RuleNoAccountingPeriod,
      "INVALID_PAYLOAD"             -> StandardDownstreamError,
      "SERVER_ERROR"                -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"         -> StandardDownstreamError,
      "INVALID_CORRELATIONID"       -> StandardDownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            MockedLossClaimConnector
              .createLossClaim(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "MESSAGE", v.httpStatus))))))

            await(service.createLossClaim(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }
}
