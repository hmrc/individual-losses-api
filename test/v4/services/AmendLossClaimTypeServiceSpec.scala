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

package v4.services

import common.errors.{
  ClaimIdFormatError,
  RuleCSFHLClaimNotSupportedError,
  RuleClaimTypeNotChanged,
  RuleOutsideAmendmentWindow,
  RuleTypeOfClaimInvalid,
  TaxYearClaimedForFormatError
}
import shared.models.domain.{Nino, TaxYear, Timestamp}
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v4.connectors.{MockAmendLossClaimTypeConnector, MockRetrieveLossClaimConnector}
import v4.models.domain.lossClaim.{ClaimId, TypeOfClaim, TypeOfLoss}
import v4.models.request.amendLossClaimType.{AmendLossClaimTypeRequestBody, AmendLossClaimTypeRequestData}
import v4.models.request.retrieveLossClaim.RetrieveLossClaimRequestData
import v4.models.response.amendLossClaimType.AmendLossClaimTypeResponse
import v4.models.response.retrieveLossClaim.RetrieveLossClaimResponse

import scala.concurrent.Future

class AmendLossClaimTypeServiceSpec extends ServiceSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  val requestBody: AmendLossClaimTypeRequestBody = AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

  val amendLossClaimResponse: AmendLossClaimTypeResponse =
    AmendLossClaimTypeResponse(
      "2019-20",
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      "XKIS00000000988",
      Some(1),
      Timestamp("2018-07-13T12:13:48.763Z")
    )

  val retrieveLossClaimResponse: RetrieveLossClaimResponse =
    RetrieveLossClaimResponse(
      "2019-20",
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      "XKIS00000000988",
      Some(1),
      Timestamp("2018-07-13T12:13:48.763Z")
    )

  val taxYear: TaxYear = TaxYear.fromMtd(retrieveLossClaimResponse.taxYearClaimedFor)

  trait Test extends MockAmendLossClaimTypeConnector with MockRetrieveLossClaimConnector {
    lazy val service = new AmendLossClaimTypeService(mockRetrieveLossClaimConnector, mockAmendLossClaimTypeConnector)
  }

  "amend LossClaim" when {
    lazy val amendRequest: AmendLossClaimTypeRequestData   = AmendLossClaimTypeRequestData(Nino(nino), ClaimId(claimId), requestBody)
    lazy val retrieveRequest: RetrieveLossClaimRequestData = RetrieveLossClaimRequestData(Nino(nino), ClaimId(claimId))

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockRetrieveLossClaimConnector
          .retrieveLossClaim(request = retrieveRequest, isAmendRequest = true)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveLossClaimResponse))))

        MockAmendLossClaimTypeConnector
          .amendLossClaimType(amendRequest, taxYear)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, amendLossClaimResponse))))

        await(service.amendLossClaimType(amendRequest)) shouldBe Right(ResponseWrapper(correlationId, amendLossClaimResponse))
      }
    }

    "return that wrapped error as-is" when {
      "the retrieve connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))

        MockRetrieveLossClaimConnector
          .retrieveLossClaim(request = retrieveRequest, isAmendRequest = true)
          .returns(Future.successful(Left(downstreamResponse)))

        await(service.amendLossClaimType(amendRequest)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }

      "the amend connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))

        MockRetrieveLossClaimConnector
          .retrieveLossClaim(request = retrieveRequest, isAmendRequest = true)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveLossClaimResponse))))

        MockAmendLossClaimTypeConnector.amendLossClaimType(amendRequest, taxYear).returns(Future.successful(Left(downstreamResponse)))

        await(service.amendLossClaimType(amendRequest)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceErrorFromRetrieveConnector(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service when the retrieve connector call fails" in new Test {
          MockRetrieveLossClaimConnector
            .retrieveLossClaim(request = retrieveRequest, isAmendRequest = true)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.amendLossClaimType(amendRequest))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      def serviceErrorFromAmendConnector(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service when the amend connector call fails" in new Test {
          MockRetrieveLossClaimConnector
            .retrieveLossClaim(request = retrieveRequest, isAmendRequest = true)
            .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveLossClaimResponse))))

          MockAmendLossClaimTypeConnector
            .amendLossClaimType(amendRequest, taxYear)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.amendLossClaimType(amendRequest))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val retrieveErrors: Seq[(String, MtdError)] = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
        "NOT_FOUND"                 -> NotFoundError,
        "INVALID_CORRELATIONID"     -> InternalError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError,
        "UNEXPECTED_ERROR"          -> InternalError
      )

      val amendIfsErrors: Seq[(String, MtdError)] = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
        "INVALID_PAYLOAD"           -> InternalError,
        "INVALID_CLAIM_TYPE"        -> RuleTypeOfClaimInvalid,
        "NOT_FOUND"                 -> NotFoundError,
        "CONFLICT"                  -> RuleClaimTypeNotChanged,
        "INVALID_CORRELATIONID"     -> InternalError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError,
        "UNEXPECTED_ERROR"          -> InternalError
      )

      val amendHipErrors: Seq[(String, MtdError)] = List(
        "1117" -> TaxYearClaimedForFormatError,
        "1215" -> NinoFormatError,
        "1216" -> InternalError,
        "1220" -> ClaimIdFormatError,
        "5010" -> NotFoundError,
        "1000" -> InternalError,
        "1105" -> RuleTypeOfClaimInvalid,
        "1127" -> RuleCSFHLClaimNotSupportedError,
        "1228" -> RuleClaimTypeNotChanged,
        "4200" -> RuleOutsideAmendmentWindow,
        "5000" -> RuleTaxYearNotSupportedError
      )

      retrieveErrors.foreach(args => (serviceErrorFromRetrieveConnector _).tupled(args))
      (amendIfsErrors ++ amendHipErrors).foreach(args => (serviceErrorFromAmendConnector _).tupled(args))

    }
  }

}
