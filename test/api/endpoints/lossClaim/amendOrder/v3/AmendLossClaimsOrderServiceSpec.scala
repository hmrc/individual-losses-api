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

package api.endpoints.lossClaim.amendOrder.v3

import api.endpoints.lossClaim.amendOrder.v3.model.Claim
import api.endpoints.lossClaim.amendOrder.v3.request.{AmendLossClaimsOrderRequest, AmendLossClaimsOrderRequestBody}
import api.endpoints.lossClaim.amendOrder.v3.response.AmendLossClaimsOrderResponse
import api.endpoints.lossClaim.connector.v3.MockLossClaimConnector
import api.endpoints.lossClaim.domain.v3.TypeOfClaim
import api.models.ResponseWrapper
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import api.models.errors.v3.{RuleInvalidSequenceStart, RuleLossClaimsMissing, RuleSequenceOrderBroken}
import api.services.ServiceSpec
import api.services.v3.Outcomes.AmendLossClaimsOrderOutcome

import scala.concurrent.Future

class AmendLossClaimsOrderServiceSpec extends ServiceSpec {

  val nino: String                            = "AA123456A"
  val taxYear: TaxYear                        = TaxYear.fromMtd("2019-20")
  override implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val lossClaimsOrder: AmendLossClaimsOrderRequestBody = AmendLossClaimsOrderRequestBody(
    TypeOfClaim.`carry-sideways`,
    Seq(Claim("1234568790ABCDE", 1), Claim("1234568790ABCDF", 2))
  )

  trait Test extends MockLossClaimConnector {
    lazy val service = new AmendLossClaimsOrderService(connector)
  }

  "amend LossClaimsOrder" when {
    lazy val request = AmendLossClaimsOrderRequest(Nino(nino), taxYear, lossClaimsOrder)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {

        val downstreamResponse: ResponseWrapper[Unit]               = ResponseWrapper(correlationId, ())
        val expected: ResponseWrapper[AmendLossClaimsOrderResponse] = ResponseWrapper(correlationId, AmendLossClaimsOrderResponse())

        MockedLossClaimConnector
          .amendLossClaimsOrder(request)
          .returns(Future.successful(Right(downstreamResponse)))

        await(service.amendLossClaimsOrder(request)) shouldBe Right(expected)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message")
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedLossClaimConnector.amendLossClaimsOrder(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.amendLossClaimsOrder(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "one of the errors is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: ResponseWrapper[MultipleErrors] = ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, ServiceUnavailableError)))
        MockedLossClaimConnector.amendLossClaimsOrder(request).returns(Future.successful(Left(expected)))
        val result: AmendLossClaimsOrderOutcome = await(service.amendLossClaimsOrder(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), StandardDownstreamError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit = {
        s"downstream returns $downstreamErrorCode" in new Test {
          MockedLossClaimConnector
            .amendLossClaimsOrder(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(downstreamErrorCode, "MESSAGE", error.httpStatus))))))

          val result = await(service.amendLossClaimsOrder(request))
          result shouldBe Left(ErrorWrapper(Some(correlationId), error))
        }
      }

      val errors = Seq(
        ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError),
        ("INVALID_TAXYEAR", TaxYearFormatError),
        ("CONFLICT_SEQUENCE_START", RuleInvalidSequenceStart),
        ("CONFLICT_NOT_SEQUENTIAL", RuleSequenceOrderBroken),
        ("CONFLICT_NOT_FULL_LIST", RuleLossClaimsMissing),
        ("INVALID_PAYLOAD", StandardDownstreamError),
        ("UNPROCESSABLE_ENTITY", NotFoundError),
        ("SERVER_ERROR", StandardDownstreamError),
        ("SERVICE_UNAVAILABLE", StandardDownstreamError)
      )

      val extraTysErrors = Seq(
        ("INVALID_TAX_YEAR", TaxYearFormatError),
        ("INVALID_CORRELATIONID", StandardDownstreamError),
        ("NOT_FOUND", NotFoundError),
        ("NOT_SEQUENTIAL", RuleSequenceOrderBroken),
        ("SEQUENCE_START", RuleInvalidSequenceStart),
        ("NO_FULL_LIST", RuleLossClaimsMissing),
        ("CLAIM_NOT_FOUND", NotFoundError),
        ("TAX_YEAR_NOT_SUPPORTED", RuleTaxYearNotSupportedError)
      )

      (errors ++ extraTysErrors).foreach(args => (serviceError _).tupled(args))
    }
  }
}
