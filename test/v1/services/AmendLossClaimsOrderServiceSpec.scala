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

import uk.gov.hmrc.domain.Nino
import v1.mocks.connectors.MockLossClaimConnector
import v1.models.des.{AmendLossClaimsOrderResponse, ReliefClaimed}
import v1.models.domain.{Claim, LossClaimsList}
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData.{AmendLossClaimsOrderRequest, DesTaxYear}

import scala.concurrent.Future


class AmendLossClaimsOrderServiceSpec extends ServiceSpec {



  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino   = Nino("AA123456A")
  val taxYear = DesTaxYear.fromMtd("2019-20")

  val lossClaimsOrder = LossClaimsList(ReliefClaimed.`CSGI`,
                            Seq(Claim("1234568790ABCDE", 1),
                                Claim("1234568790ABCDF", 2)))

  val serviceUnavailableError = MtdError("SERVICE_UNAVAILABLE", "doesn't matter")

  trait Test extends MockLossClaimConnector {
    lazy val service = new AmendLossClaimsOrderService(connector)
  }

  "amend LossClaimsOrder" when {
    lazy val request = AmendLossClaimsOrderRequest(nino, taxYear, lossClaimsOrder)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {

        val desResponse = DesResponse(correlationId, ())
        val expected = DesResponse(correlationId, AmendLossClaimsOrderResponse())

        MockedLossClaimConnector.amendLossClaimsOrder(request)
          .returns(Future.successful(Right(desResponse)))

        await(service.amendLossClaimsOrder(request)) shouldBe Right(expected)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError   = MtdError("SOME_CODE", "some message")
        val desResponse = DesResponse(correlationId, OutboundError(someError))
        MockedLossClaimConnector.amendLossClaimsOrder(request).returns(Future.successful(Left(desResponse)))

        await(service.amendLossClaimsOrder(request)) shouldBe Left(ErrorWrapper(Some(correlationId), someError, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, serviceUnavailableError)))
        MockedLossClaimConnector.amendLossClaimsOrder(request).returns(Future.successful(Left(expected)))
        val result: AmendLossClaimsOrderOutcome = await(service.amendLossClaimsOrder(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAXYEAR"           -> TaxYearFormatError,
      "CONFLICT_SEQUENCE_START"   -> RuleInvalidSequenceStart,
      "CONFLICT_NOT_SEQUENTIAL"   -> RuleSequenceOrderBroken,
      "CONFLICT_NOT_FULL_LIST"    -> RuleLossClaimsMissing,
      "INVALID_PAYLOAD"           -> DownstreamError,
      "UNPROCESSABLE_ENTITY"      -> NotFoundError,
      "SERVER_ERROR"              -> DownstreamError,
      "SERVICE_UNAVAILABLE"       -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            MockedLossClaimConnector
              .amendLossClaimsOrder(request)
              .returns(Future.successful(Left(DesResponse(correlationId, SingleError(MtdError(k, "MESSAGE"))))))

            await(service.amendLossClaimsOrder(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }

}
