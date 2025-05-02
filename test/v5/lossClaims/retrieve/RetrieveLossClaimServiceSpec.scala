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

package v5.lossClaims.retrieve

import common.errors.ClaimIdFormatError
import shared.models.domain.{Nino, Timestamp}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v5.lossClaims.common.models.{ClaimId, TypeOfClaim, TypeOfLoss}
import v5.lossClaims.retrieve.def1.model.request.Def1_RetrieveLossClaimRequestData
import v5.lossClaims.retrieve.def1.model.response.Def1_RetrieveLossClaimResponse
import v5.lossClaims.retrieve.model.request.RetrieveLossClaimRequestData
import v5.lossClaims.retrieve.model.response.RetrieveLossClaimResponse

import scala.concurrent.Future

class RetrieveLossClaimServiceSpec extends ServiceSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  trait Test extends MockRetrieveLossClaimConnector {
    lazy val service = new RetrieveLossClaimService(mockRetrieveLossClaimConnector)
  }

  lazy val request: RetrieveLossClaimRequestData = Def1_RetrieveLossClaimRequestData(Nino(nino), ClaimId(claimId))

  "retrieve loss claim" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[RetrieveLossClaimResponse] =
          ResponseWrapper(
            correlationId,
            Def1_RetrieveLossClaimResponse(
              "2019-20",
              TypeOfLoss.`self-employment`,
              TypeOfClaim.`carry-forward`,
              "selfEmploymentId",
              Some(1),
              Timestamp("2018-07-13T12:13:48.763Z")
            )
          )
        MockRetrieveLossClaimConnector
          .retrieveLossClaim(request = request, isAmendRequest = false)
          .returns(Future.successful(Right(downstreamResponse)))

        await(service.retrieveLossClaim(request)) shouldBe Right(downstreamResponse)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockRetrieveLossClaimConnector
          .retrieveLossClaim(request = request, isAmendRequest = false)
          .returns(Future.successful(Left(downstreamResponse)))

        await(service.retrieveLossClaim(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockRetrieveLossClaimConnector
            .retrieveLossClaim(request = request, isAmendRequest = false)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.retrieveLossClaim(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors: Seq[(String, MtdError)] = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
        "NOT_FOUND"                 -> NotFoundError,
        "INVALID_CORRELATIONID"     -> InternalError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError
      )

      errors.foreach(args => (serviceError _).tupled(args))
    }
  }

}
