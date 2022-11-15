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

package api.endpoints.lossClaim.list.v3

import api.endpoints.lossClaim.connector.v3.MockLossClaimConnector
import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.endpoints.lossClaim.list.v3.request.ListLossClaimsRequest
import api.endpoints.lossClaim.list.v3.response.{ListLossClaimsItem, ListLossClaimsResponse}
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._
import api.services.ServiceSpec

import scala.concurrent.Future

class ListLossClaimsServiceSpec extends ServiceSpec {

  val nino: String                            = "AA123456A"
  val claimId: String                         = "AAZZ1234567890a"
  override implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  trait Test extends MockLossClaimConnector {
    lazy val service = new ListLossClaimsService(connector)
  }

  lazy val request: ListLossClaimsRequest = ListLossClaimsRequest(Nino(nino), None, None, None, None)

  "retrieve the list of bf losses" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[ListLossClaimsResponse[ListLossClaimsItem]] =
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(
              Seq(
                ListLossClaimsItem("testId",
                                   TypeOfClaim.`carry-sideways`,
                                   TypeOfLoss.`self-employment`,
                                   "2020",
                                   "claimId",
                                   Some(1),
                                   "2020-07-13T12:13:48.763Z"),
                ListLossClaimsItem("testId2",
                                   TypeOfClaim.`carry-sideways`,
                                   TypeOfLoss.`self-employment`,
                                   "2020",
                                   "claimId2",
                                   Some(1),
                                   "2020-07-13T12:13:48.763Z")
              ))
          )
        MockedLossClaimConnector.listLossClaims(request).returns(Future.successful(Right(downstreamResponse)))

        await(service.listLossClaims(request)) shouldBe Right(downstreamResponse)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedLossClaimConnector.listLossClaims(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.listLossClaims(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "return a downstream error" when {
      "the connector call returns a single downstream error" in new Test {
        val downstreamResponse: ResponseWrapper[SingleError] = ResponseWrapper(correlationId, SingleError(StandardDownstreamError))
        val expected: ErrorWrapper                           = ErrorWrapper(Some(correlationId), StandardDownstreamError, None)
        MockedLossClaimConnector.listLossClaims(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.listLossClaims(request)) shouldBe Left(expected)
      }

      "the connector call returns multiple errors including a downstream error" in new Test {
        val downstreamResponse: ResponseWrapper[MultipleErrors] =
          ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, StandardDownstreamError)))
        val expected: ErrorWrapper = ErrorWrapper(Some(correlationId), StandardDownstreamError, None)
        MockedLossClaimConnector.listLossClaims(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.listLossClaims(request)) shouldBe Left(expected)
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAXYEAR"           -> TaxYearFormatError,
      "INVALID_INCOMESOURCEID"    -> BusinessIdFormatError,
      "INVALID_INCOMESOURCETYPE"  -> TypeOfLossFormatError,
      "INVALID_CLAIM_TYPE"        -> TypeOfClaimFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "INVALID_CORRELATIONID"     -> StandardDownstreamError,
      "SERVER_ERROR"              -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
    ).foreach {
      case (k, v) =>
        s"return a ${v.code} error" when {
          s"the connector call returns $k" in new Test {
            MockedLossClaimConnector
              .listLossClaims(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "doesn't matter", v.httpStatus))))))

            await(service.listLossClaims(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }
}
