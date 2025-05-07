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

package v5.lossClaims.delete

import common.errors.ClaimIdFormatError
import shared.models.domain.{Nino, TaxYear, Timestamp}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v5.lossClaims.common.models.{ClaimId, TypeOfClaim, TypeOfLoss}
import v5.lossClaims.delete.def1.model.request.Def1_DeleteLossClaimRequestData
import v5.lossClaims.delete.model.request.DeleteLossClaimRequestData
import v5.lossClaims.retrieve.MockRetrieveLossClaimConnector
import v5.lossClaims.retrieve.def1.model.request.Def1_RetrieveLossClaimRequestData
import v5.lossClaims.retrieve.def1.model.response.Def1_RetrieveLossClaimResponse
import v5.lossClaims.retrieve.model.request.RetrieveLossClaimRequestData

import scala.concurrent.Future

class DeleteLossClaimServiceSpec extends ServiceSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  val retrieveLossClaimResponse: Def1_RetrieveLossClaimResponse =
    Def1_RetrieveLossClaimResponse(
      "2019-20",
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      "XKIS00000000988",
      Some(1),
      Timestamp("2018-07-13T12:13:48.763Z")
    )

  val taxYear: TaxYear = TaxYear.fromMtd(retrieveLossClaimResponse.taxYearClaimedFor)

  trait Test extends MockDeleteLossClaimConnector with MockRetrieveLossClaimConnector {
    lazy val service = new DeleteLossClaimService(mockRetrieveLossClaimConnector, mockDeleteLossClaimConnector)
  }

  lazy val deleteRequest: DeleteLossClaimRequestData     = Def1_DeleteLossClaimRequestData(Nino(nino), ClaimId(claimId))
  lazy val retrieveRequest: RetrieveLossClaimRequestData = Def1_RetrieveLossClaimRequestData(Nino(nino), ClaimId(claimId))

  "Delete Loss Claim" should {
    "return a right" when {
      "the connector call is successful" in new Test {

        val downstreamResponse: ResponseWrapper[Unit] = ResponseWrapper(correlationId, ())
        val expected: ResponseWrapper[Unit]           = ResponseWrapper(correlationId, ())

        MockRetrieveLossClaimConnector
          .retrieveLossClaim(request = retrieveRequest, isAmendRequest = false)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveLossClaimResponse))))

        MockDeleteLossClaimConnector
          .deleteLossClaim(deleteRequest, taxYear)
          .returns(Future.successful(Right(downstreamResponse)))

        await(service.deleteLossClaim(deleteRequest)) shouldBe Right(expected)

      }
    }

    "return that wrapped error as-is" when {
      "the retrieve connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))

        MockRetrieveLossClaimConnector
          .retrieveLossClaim(request = retrieveRequest, isAmendRequest = false)
          .returns(Future.successful(Left(downstreamResponse)))

        await(service.deleteLossClaim(deleteRequest)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }

      "the delete connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))

        MockRetrieveLossClaimConnector
          .retrieveLossClaim(request = retrieveRequest, isAmendRequest = false)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveLossClaimResponse))))

        MockDeleteLossClaimConnector.deleteLossClaim(deleteRequest, taxYear).returns(Future.successful(Left(downstreamResponse)))

        await(service.deleteLossClaim(deleteRequest)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceErrorFromRetrieveConnector(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service when the retrieve connector call fails" in new Test {
          MockRetrieveLossClaimConnector
            .retrieveLossClaim(request = retrieveRequest, isAmendRequest = false)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.deleteLossClaim(deleteRequest))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      def serviceErrorFromDeleteConnector(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service when the delete connector call fails" in new Test {
          MockRetrieveLossClaimConnector
            .retrieveLossClaim(request = retrieveRequest, isAmendRequest = false)
            .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveLossClaimResponse))))

          MockDeleteLossClaimConnector
            .deleteLossClaim(deleteRequest, taxYear)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.deleteLossClaim(deleteRequest))
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

      val deleteErrors: Seq[(String, MtdError)] = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
        "NOT_FOUND"                 -> NotFoundError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError,
        "UNEXPECTED_ERROR"          -> InternalError
      )

      retrieveErrors.foreach(args => (serviceErrorFromRetrieveConnector _).tupled(args))
      deleteErrors.foreach(args => (serviceErrorFromDeleteConnector _).tupled(args))
    }
  }

}
