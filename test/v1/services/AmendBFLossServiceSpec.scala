/*
 * Copyright 2019 HM Revenue & Customs
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
import v1.mocks.connectors.MockDesConnector
import v1.models.des.AmendBFLossResponse
import v1.models.domain.AmendBFLoss
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData.AmendBFLossRequest

import scala.concurrent.Future

class AmendBFLossServiceSpec extends ServiceSpec {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino = Nino("AA123456A")
  val lossId = "AAZZ1234567890a"

  val bfLoss = AmendBFLoss(256.78)

  val bfLossResponse = AmendBFLossResponse(Some("XKIS00000000988"), "self-employment", 256.78, "2019-20")

  val serviceUnavailableError = MtdError("SERVICE_UNAVAILABLE", "doesn't matter")

  trait Test extends MockDesConnector {
    lazy val service = new AmendBFLossService(connector)
  }

  "amend BFLoss" when {
    lazy val request = AmendBFLossRequest(nino, lossId, bfLoss)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockedDesConnector
          .amendBFLoss(request)
          .returns(Future.successful(Right(DesResponse(correlationId, bfLossResponse))))

        await(service.amendBFLoss(request)) shouldBe Right(DesResponse(correlationId, bfLossResponse))
      }
    }

    "the connector returns an outbound error" should {
      "return that outbound error as-is" in new Test {

        val desResponse = DesResponse(correlationId, OutboundError(DownstreamError))
        val expected = DesResponse(correlationId, OutboundError(DownstreamError))
        MockedDesConnector.amendBFLoss(request).returns(Future.successful(Left(desResponse)))
        val result = await(service.amendBFLoss(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), expected.responseData.error, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, serviceUnavailableError)))
        MockedDesConnector.amendBFLoss(request).returns(Future.successful(Left(expected)))
        val result = await(service.amendBFLoss(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID"  -> NinoFormatError,
      "INVALID_LOSS_ID"            -> LossIdFormatError,
      "NOT_FOUND"                  -> NotFoundError,
      "INVALID_PAYLOAD"            -> DownstreamError,
      "SERVER_ERROR"               -> DownstreamError,
      "SERVICE_UNAVAILABLE"        -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            MockedDesConnector
              .amendBFLoss(request)
              .returns(Future.successful(Left(DesResponse(correlationId, SingleError(MtdError(k, "MESSAGE"))))))

            await(service.amendBFLoss(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }
}