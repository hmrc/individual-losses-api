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

import api.models.ResponseWrapper
import api.models.domain.{Nino, Timestamp}
import api.models.errors._
import api.services.ServiceSpec
import v3.connectors.MockRetrieveLossClaimConnector
import v3.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}
import v3.models.request.retrieveLossClaim.RetrieveLossClaimRequest
import v3.models.response.retrieveLossClaim.RetrieveLossClaimResponse

import scala.concurrent.Future

class RetrieveLossClaimServiceSpec extends ServiceSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  trait Test extends MockRetrieveLossClaimConnector {
    lazy val service = new RetrieveLossClaimService(connector)
  }

  lazy val request: RetrieveLossClaimRequest = RetrieveLossClaimRequest(Nino(nino), claimId)

  "retrieve loss claim" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[RetrieveLossClaimResponse] =
          ResponseWrapper(
            correlationId,
            RetrieveLossClaimResponse(
              "2019-20",
              TypeOfLoss.`self-employment`,
              TypeOfClaim.`carry-forward`,
              "selfEmploymentId",
              Some(1),
              Timestamp("2018-07-13T12:13:48.763Z")
            )
          )
        MockRetrieveLossClaimConnector.retrieveLossClaim(request).returns(Future.successful(Right(downstreamResponse)))

        await(service.retrieveLossClaim(request)) shouldBe Right(downstreamResponse)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockRetrieveLossClaimConnector.retrieveLossClaim(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.retrieveLossClaim(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockRetrieveLossClaimConnector
            .retrieveLossClaim(request)
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
