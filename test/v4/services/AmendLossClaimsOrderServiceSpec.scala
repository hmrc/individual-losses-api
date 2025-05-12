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

package v4.services

import common.errors.{RuleInvalidSequenceStart, RuleLossClaimsMissing, RuleOutsideAmendmentWindow, RuleSequenceOrderBroken}
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v4.connectors.MockAmendLossClaimsConnector
import v4.models.domain.lossClaim.TypeOfClaim
import v4.models.request.amendLossClaimsOrder.{AmendLossClaimsOrderRequestBody, AmendLossClaimsOrderRequestData, Claim}
import v4.models.response.amendLossClaimsOrder.AmendLossClaimsOrderResponse

import scala.concurrent.Future

class AmendLossClaimsOrderServiceSpec extends ServiceSpec {

  val nino: String     = "AA123456A"
  val taxYear: TaxYear = TaxYear.fromMtd("2019-20")

  val lossClaimsOrder: AmendLossClaimsOrderRequestBody = AmendLossClaimsOrderRequestBody(
    TypeOfClaim.`carry-sideways`,
    Seq(Claim("1234568790ABCDE", 1), Claim("1234568790ABCDF", 2))
  )

  trait Test extends MockAmendLossClaimsConnector {
    lazy val service = new AmendLossClaimsOrderService(mockAmendLossClaimsConnector)
  }

  "amend LossClaimsOrder" when {
    lazy val request = AmendLossClaimsOrderRequestData(Nino(nino), taxYear, lossClaimsOrder)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {

        val downstreamResponse: ResponseWrapper[Unit]               = ResponseWrapper(correlationId, ())
        val expected: ResponseWrapper[AmendLossClaimsOrderResponse] = ResponseWrapper(correlationId, AmendLossClaimsOrderResponse())

        MockAmendLossClaimsConnector
          .amendLossClaimsOrder(request)
          .returns(Future.successful(Right(downstreamResponse)))

        await(service.amendLossClaimsOrder(request)) shouldBe Right(expected)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockAmendLossClaimsConnector.amendLossClaimsOrder(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.amendLossClaimsOrder(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit = {
        s"downstream returns $downstreamErrorCode" in new Test {
          MockAmendLossClaimsConnector
            .amendLossClaimsOrder(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.amendLossClaimsOrder(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }
      }

      val errors = List(
        ("1215", NinoFormatError),
        ("1117", TaxYearFormatError),
        ("1216", InternalError),
        ("1000", InternalError),
        ("1108", NotFoundError),
        ("1109", RuleSequenceOrderBroken),
        ("1110", RuleInvalidSequenceStart),
        ("1111", RuleLossClaimsMissing),
        ("4200", RuleOutsideAmendmentWindow),
        ("5000", RuleTaxYearNotSupportedError)
      )

      errors.foreach(args => (serviceError _).tupled(args))
    }
  }

}
