/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.services

import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import v1.controllers.UserRequest
import v1.mocks.connectors.MockLossClaimConnector
import v1.models.auth.UserDetails
import v1.models.des.{ListLossClaimsResponse, LossClaimId}
import v1.models.domain.TypeOfClaim
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData.ListLossClaimsRequest

import scala.concurrent.Future

class ListLossClaimsServiceSpec extends ServiceSpec {

  val nino: Nino = Nino("AA123456A")
  val claimId = "AAZZ1234567890a"

  trait Test extends MockLossClaimConnector {
    implicit val ur = UserRequest(UserDetails("", "",None), FakeRequest())
    lazy val service = new ListLossClaimsService(connector)
  }

  lazy val request: ListLossClaimsRequest = ListLossClaimsRequest(nino, None, None, None, None)

  "retrieve the list of bf losses" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val desResponse: DesResponse[ListLossClaimsResponse[LossClaimId]] = DesResponse(correlationId,
          ListLossClaimsResponse(Seq(LossClaimId("testId", Some(1), TypeOfClaim.`carry-sideways`),
          LossClaimId("testId2", Some(2), TypeOfClaim.`carry-sideways`))))
        MockedLossClaimConnector.listLossClaims(request).returns(Future.successful(Right(desResponse)))

        await(service.listLossClaims(request)) shouldBe Right(desResponse)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError = MtdError("SOME_CODE", "some message")
        val desResponse: DesResponse[OutboundError] = DesResponse(correlationId, OutboundError(someError))
        MockedLossClaimConnector.listLossClaims(request).returns(Future.successful(Left(desResponse)))

        await(service.listLossClaims(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "return a downstream error" when {
      "the connector call returns a single downstream error" in new Test {
        val desResponse: DesResponse[SingleError] = DesResponse(correlationId, SingleError(DownstreamError))
        val expected: ErrorWrapper = ErrorWrapper(correlationId, DownstreamError, None)
        MockedLossClaimConnector.listLossClaims(request).returns(Future.successful(Left(desResponse)))

        await(service.listLossClaims(request)) shouldBe Left(expected)
      }

      "the connector call returns multiple errors including a downstream error" in new Test {
        val desResponse: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, DownstreamError)))
        val expected: ErrorWrapper = ErrorWrapper(correlationId, DownstreamError, None)
        MockedLossClaimConnector.listLossClaims(request).returns(Future.successful(Left(desResponse)))

        await(service.listLossClaims(request)) shouldBe Left(expected)
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAXYEAR" -> TaxYearFormatError,
      "INVALID_INCOMESOURCEID" -> SelfEmploymentIdFormatError,
      "INVALID_INCOMESOURCETYPE" -> TypeOfLossFormatError,
      "INVALID_CLAIMTYPE" -> ClaimTypeFormatError,
      "NOT_FOUND" -> NotFoundError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"return a ${v.code} error" when {
          s"the connector call returns $k" in new Test {
            MockedLossClaimConnector
              .listLossClaims(request)
              .returns(Future.successful(Left(DesResponse(correlationId, SingleError(MtdError(k, "doesn't matter"))))))

            await(service.listLossClaims(request)) shouldBe Left(ErrorWrapper(correlationId, v, None))
          }
        }
    }
  }

}
