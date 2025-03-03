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

package v6.lossClaims.amendType

import common.errors.{
  ClaimIdFormatError,
  RuleCSFHLClaimNotSupportedError,
  RuleClaimTypeNotChanged,
  RuleOutsideAmendmentWindow,
  RuleTypeOfClaimInvalid
}
import shared.models.domain.{Nino, TaxYear, Timestamp}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v6.lossClaims.amendType.def1.model.request.{Def1_AmendLossClaimTypeRequestBody, Def1_AmendLossClaimTypeRequestData}
import v6.lossClaims.amendType.def1.model.response.Def1_AmendLossClaimTypeResponse
import v6.lossClaims.amendType.model.response.AmendLossClaimTypeResponse
import v6.lossClaims.common.models.{ClaimId, TypeOfClaim, TypeOfLoss}

import scala.concurrent.Future

class AmendLossClaimTypeServiceSpec extends ServiceSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890a"
  val taxYear         = "2019-20"

  val requestBody: Def1_AmendLossClaimTypeRequestBody = Def1_AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

  val lossClaimResponse: AmendLossClaimTypeResponse =
    Def1_AmendLossClaimTypeResponse(
      "2019-20",
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      "XKIS00000000988",
      Some(1),
      Timestamp("2018-07-13T12:13:48.763Z")
    )

  trait Test extends MockAmendLossClaimTypeConnector {
    lazy val service = new AmendLossClaimTypeService(mockAmendLossClaimTypeConnector)
    val request      = Def1_AmendLossClaimTypeRequestData(Nino(nino), ClaimId(claimId), requestBody, TaxYear.fromMtd(taxYear))
  }

  "amend LossClaim" when {

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockAmendLossClaimTypeConnector
          .amendLossClaimType(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, lossClaimResponse))))

        await(service.amendLossClaimType(request)) shouldBe Right(ResponseWrapper(correlationId, lossClaimResponse))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))

        MockAmendLossClaimTypeConnector.amendLossClaimType(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.amendLossClaimType(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockAmendLossClaimTypeConnector
            .amendLossClaimType(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.amendLossClaimType(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors: Seq[(String, MtdError)] = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
        "INVALID_PAYLOAD"           -> InternalError,
        "INVALID_CLAIM_TYPE"        -> RuleTypeOfClaimInvalid,
        "CSFHL_CLAIM_NOT_SUPPORTED" -> RuleCSFHLClaimNotSupportedError,
        "NOT_FOUND"                 -> NotFoundError,
        "CONFLICT"                  -> RuleClaimTypeNotChanged,
        "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
        "INVALID_CORRELATIONID"     -> InternalError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError,
        "UNEXPECTED_ERROR"          -> InternalError
      )

      errors.foreach(args => (serviceError _).tupled(args))

    }
  }

}
