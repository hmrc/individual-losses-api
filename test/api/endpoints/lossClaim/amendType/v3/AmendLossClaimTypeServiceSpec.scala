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

package api.endpoints.lossClaim.amendType.v3

import api.endpoints.lossClaim.amendType.v3.request.{ AmendLossClaimTypeRequest, AmendLossClaimTypeRequestBody }
import api.endpoints.lossClaim.amendType.v3.response.AmendLossClaimTypeResponse
import api.endpoints.lossClaim.connector.v3.MockLossClaimConnector
import api.endpoints.lossClaim.domain.v3.{ TypeOfClaim, TypeOfLoss }
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._
import api.services.ServiceSpec
import api.services.v3.Outcomes.AmendLossClaimTypeOutcome

import scala.concurrent.Future

class AmendLossClaimTypeServiceSpec extends ServiceSpec {

  val nino: String                            = "AA123456A"
  val claimId: String                         = "AAZZ1234567890a"
  override implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val requestBody: AmendLossClaimTypeRequestBody = AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

  val lossClaimResponse: AmendLossClaimTypeResponse =
    AmendLossClaimTypeResponse(
      "2019-20",
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      "XKIS00000000988",
      Some(1),
      "lastModified"
    )

  trait Test extends MockLossClaimConnector {
    lazy val service = new AmendLossClaimTypeService(connector)
  }

  "amend LossClaim" when {
    lazy val request = AmendLossClaimTypeRequest(Nino(nino), claimId, requestBody)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockedLossClaimConnector
          .amendLossClaimType(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, lossClaimResponse))))

        await(service.amendLossClaimType(request)) shouldBe Right(ResponseWrapper(correlationId, lossClaimResponse))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message")
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedLossClaimConnector.amendLossClaimType(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.amendLossClaimType(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "one of the errors from downstream is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: ResponseWrapper[MultipleErrors] = ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, ServiceUnavailableError)))
        MockedLossClaimConnector.amendLossClaimType(request).returns(Future.successful(Left(expected)))
        val result: AmendLossClaimTypeOutcome = await(service.amendLossClaimType(request))
        result shouldBe Left(ErrorWrapper(correlationId, StandardDownstreamError, None))
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
      "INVALID_PAYLOAD"           -> StandardDownstreamError,
      "INVALID_CLAIM_TYPE"        -> RuleTypeOfClaimInvalidForbidden,
      "NOT_FOUND"                 -> NotFoundError,
      "CONFLICT"                  -> RuleClaimTypeNotChanged,
      "INVALID_CORRELATIONID"     -> StandardDownstreamError,
      "SERVER_ERROR"              -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"       -> StandardDownstreamError,
      "UNEXPECTED_ERROR"          -> StandardDownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            MockedLossClaimConnector
              .amendLossClaimType(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "MESSAGE"))))))

            await(service.amendLossClaimType(request)) shouldBe Left(ErrorWrapper(correlationId, v, None))
          }
        }
    }
  }
}
