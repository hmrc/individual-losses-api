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

package v6.lossClaims.create

import common.errors._
import shared.models.domain.Nino
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.{ServiceOutcome, ServiceSpec}
import v6.lossClaims.common.models._
import v6.lossClaims.create.def1.model.request.{Def1_CreateLossClaimRequestBody, Def1_CreateLossClaimRequestData}
import v6.lossClaims.create.def1.model.response.Def1_CreateLossClaimResponse
import v6.lossClaims.create.model.response.CreateLossClaimResponse

import scala.concurrent.Future

class CreateLossClaimServiceSpec extends ServiceSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  val lossClaim: Def1_CreateLossClaimRequestBody =
    Def1_CreateLossClaimRequestBody("2018", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, "XKIS00000000988")

  trait Test extends MockCreateLossClaimConnector {
    lazy val service = new CreateLossClaimService(connector)
  }

  "create LossClaim" when {
    lazy val request = Def1_CreateLossClaimRequestData(Nino(nino), lossClaim)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockCreateLossClaimConnector
          .createLossClaim(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, Def1_CreateLossClaimResponse(claimId)))))

        await(service.createLossClaim(request)) shouldBe Right(ResponseWrapper(correlationId, Def1_CreateLossClaimResponse(claimId)))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockCreateLossClaimConnector.createLossClaim(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.createLossClaim(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "one of the errors from downstream is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: ResponseWrapper[DownstreamErrors] =
          ResponseWrapper(
            correlationId,
            DownstreamErrors(Seq(DownstreamErrorCode(NinoFormatError.code), DownstreamErrorCode(ServiceUnavailableError.code))))

        MockCreateLossClaimConnector.createLossClaim(request).returns(Future.successful(Left(expected)))
        val result: ServiceOutcome[CreateLossClaimResponse] = await(service.createLossClaim(request))
        result shouldBe Left(ErrorWrapper(correlationId, InternalError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockCreateLossClaimConnector
            .createLossClaim(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.createLossClaim(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))

        }

      val ifsErrors: Seq[(String, MtdError)] = List(
        "INVALID_TAXABLE_ENTITY_ID"   -> NinoFormatError,
        "INVALID_TAX_YEAR"            -> TaxYearClaimedForFormatError,
        "DUPLICATE"                   -> RuleDuplicateClaimSubmissionError,
        "ACCOUNTING_PERIOD_NOT_ENDED" -> RulePeriodNotEnded,
        "INVALID_CLAIM_TYPE"          -> RuleTypeOfClaimInvalid,
        "INCOME_SOURCE_NOT_FOUND"     -> NotFoundError,
        "TAX_YEAR_NOT_SUPPORTED"      -> RuleTaxYearNotSupportedError,
        "NO_ACCOUNTING_PERIOD"        -> RuleNoAccountingPeriod,
        "OUTSIDE_AMENDMENT_WINDOW"    -> RuleOutsideAmendmentWindow,
        "INVALID_PAYLOAD"             -> InternalError,
        "SERVER_ERROR"                -> InternalError,
        "SERVICE_UNAVAILABLE"         -> InternalError,
        "INVALID_CORRELATIONID"       -> InternalError
      )

      val hipErrors: Seq[(String, MtdError)] = List(
        "1215" -> NinoFormatError,
        "1002" -> NotFoundError,
        "1117" -> TaxYearFormatError,
        "1127" -> RuleCSFHLClaimNotSupportedError,
        "1228" -> RuleDuplicateClaimSubmissionError,
        "1104" -> RulePeriodNotEnded,
        "1105" -> RuleTypeOfClaimInvalid,
        "1106" -> RuleNoAccountingPeriod,
        "1107" -> RuleTaxYearNotSupportedError,
        "5000" -> RuleTaxYearNotSupportedError
      )

      (ifsErrors ++ hipErrors).foreach(args => (serviceError _).tupled(args))
    }
  }

}
