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

package v3.services

import api.models.domain.Nino
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v3.connectors.MockCreateLossClaimConnector
import v3.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}
import v3.models.request.createLossClaim.{CreateLossClaimRequestBody, CreateLossClaimRequestData}
import v3.models.response.createLossClaim.CreateLossClaimResponse

import scala.concurrent.Future

class CreateLossClaimServiceSpec extends ServiceSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  val lossClaim: CreateLossClaimRequestBody =
    CreateLossClaimRequestBody("2018", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, "XKIS00000000988")

  trait Test extends MockCreateLossClaimConnector {
    lazy val service = new CreateLossClaimService(connector)
  }

  "create LossClaim" when {
    lazy val request = CreateLossClaimRequestData(Nino(nino), lossClaim)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockCreateLossClaimConnector
          .createLossClaim(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, CreateLossClaimResponse(claimId)))))

        await(service.createLossClaim(request)) shouldBe Right(ResponseWrapper(correlationId, CreateLossClaimResponse(claimId)))
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
        val result = await(service.createLossClaim(request))
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

      val errors: Seq[(String, MtdError)] = List(
        "INVALID_TAXABLE_ENTITY_ID"   -> NinoFormatError,
        "DUPLICATE"                   -> RuleDuplicateClaimSubmissionError,
        "ACCOUNTING_PERIOD_NOT_ENDED" -> RulePeriodNotEnded,
        "INVALID_CLAIM_TYPE"          -> RuleTypeOfClaimInvalid,
        "INCOME_SOURCE_NOT_FOUND"     -> NotFoundError,
        "TAX_YEAR_NOT_SUPPORTED"      -> RuleTaxYearNotSupportedError,
        "NO_ACCOUNTING_PERIOD"        -> RuleNoAccountingPeriod,
        "INVALID_PAYLOAD"             -> InternalError,
        "SERVER_ERROR"                -> InternalError,
        "SERVICE_UNAVAILABLE"         -> InternalError,
        "INVALID_CORRELATIONID"       -> InternalError
      )

      errors.foreach(args => (serviceError _).tupled(args))
    }
  }

}
