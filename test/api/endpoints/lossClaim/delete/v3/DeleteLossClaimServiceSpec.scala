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

package api.endpoints.lossClaim.delete.v3

import api.endpoints.lossClaim.connector.v3.MockLossClaimConnector
import api.endpoints.lossClaim.delete.v3.request.DeleteLossClaimRequest
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._
import api.services.ServiceSpec

import scala.concurrent.Future

class DeleteLossClaimServiceSpec extends ServiceSpec {

  val nino: String                            = "AA123456A"
  val claimId: String                         = "AAZZ1234567890a"
  override implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  trait Test extends MockLossClaimConnector {
    lazy val service = new DeleteLossClaimService(connector)
  }

  lazy val request: DeleteLossClaimRequest = DeleteLossClaimRequest(Nino(nino), claimId)

  "Delete Loss Claim" should {
    "return a right" when {
      "the connector call is successful" in new Test {

        val downstreamResponse: ResponseWrapper[Unit] = ResponseWrapper(correlationId, ())
        val expected: ResponseWrapper[Unit]           = ResponseWrapper(correlationId, ())

        MockedLossClaimConnector
          .deleteLossClaim(request)
          .returns(Future.successful(Right(downstreamResponse)))

        await(service.deleteLossClaim(request)) shouldBe Right(expected)

      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message")
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedLossClaimConnector.deleteLossClaim(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.deleteLossClaim(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "return a downstream error" when {
      "the connector call returns a single downstream error" in new Test {
        val downstreamResponse: ResponseWrapper[SingleError] = ResponseWrapper(correlationId, SingleError(InternalError))
        val expected: ErrorWrapper = ErrorWrapper(correlationId, InternalError, None)
        MockedLossClaimConnector.deleteLossClaim(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.deleteLossClaim(request)) shouldBe Left(expected)
      }
      "the connector call returns multiple errors including a downstream error" in new Test {
        val downstreamResponse: ResponseWrapper[MultipleErrors] =
          ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, InternalError)))
        val expected: ErrorWrapper = ErrorWrapper(correlationId, InternalError, None)
        MockedLossClaimConnector.deleteLossClaim(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.deleteLossClaim(request)) shouldBe Left(expected)
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_CLAIM_ID" -> ClaimIdFormatError,
      "NOT_FOUND" -> NotFoundError,
      "SERVER_ERROR" -> InternalError,
      "SERVICE_UNAVAILABLE" -> InternalError,
      "UNEXPECTED_ERROR" -> InternalError
    ).foreach {
      case (k, v) =>
        s"return a ${v.code} error" when {
          s"the connector call returns $k" in new Test {
            MockedLossClaimConnector
              .deleteLossClaim(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "doesn't matter", v.httpStatus))))))

            await(service.deleteLossClaim(request)) shouldBe Left(ErrorWrapper(correlationId, v, None))
          }
        }
    }
  }
}
