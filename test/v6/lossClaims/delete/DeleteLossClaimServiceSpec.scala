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

package v6.lossClaims.delete

import common.errors.{ClaimIdFormatError, RuleOutsideAmendmentWindow}
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v6.lossClaims.common.models.ClaimId
import v6.lossClaims.delete.def1.model.request.Def1_DeleteLossClaimRequestData
import v6.lossClaims.delete.model.request.DeleteLossClaimRequestData

import scala.concurrent.Future

class DeleteLossClaimServiceSpec extends ServiceSpec {

  private val nino: String    = "AA123456A"
  private val claimId: String = "AAZZ1234567890a"
  private val taxYear: String = "2019-20"

  trait Test extends MockDeleteLossClaimConnector {
    lazy val service = new DeleteLossClaimService(connector)
  }

  lazy val request: DeleteLossClaimRequestData = Def1_DeleteLossClaimRequestData(Nino(nino), ClaimId(claimId), TaxYear.fromMtd(taxYear))

  "Delete Loss Claim" should {
    "return a Right" when {
      "the connector call is successful" in new Test {

        val downstreamResponse: ResponseWrapper[Unit] = ResponseWrapper(correlationId, ())
        val expected: ResponseWrapper[Unit]           = ResponseWrapper(correlationId, ())

        MockDeleteLossClaimConnector
          .deleteLossClaim(request)
          .returns(Future.successful(Right(downstreamResponse)))

        await(service.deleteLossClaim(request)) shouldBe Right(expected)

      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockDeleteLossClaimConnector.deleteLossClaim(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.deleteLossClaim(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockDeleteLossClaimConnector
            .deleteLossClaim(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.deleteLossClaim(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors: Seq[(String, MtdError)] = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
        "INVALID_TAX_YEAR"          -> TaxYearFormatError,
        "NOT_FOUND"                 -> NotFoundError,
        "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError,
        "UNEXPECTED_ERROR"          -> InternalError
      )

      errors.foreach(args => (serviceError _).tupled(args))
    }
  }

}
