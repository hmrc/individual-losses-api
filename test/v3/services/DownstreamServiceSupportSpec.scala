/*
 * Copyright 2021 HM Revenue & Customs
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

import cats.syntax.either._
import support.UnitSpec
import v3.connectors.IfsOutcome
import v3.models.errors._
import v3.models.outcomes.ResponseWrapper

class DownstreamServiceSupportSpec extends UnitSpec with DownstreamServiceSupport {

  type D = String
  type V = String

  val ep            = "someEndpoint"
  val correlationId = "correllationId"

  val downstreamError1: MtdError        = MtdError("DOWNSTREAM_CODE1", "downstreammsg1")
  val downstreamError2: MtdError        = MtdError("DOWNSTREAM_CODE2", "downstreammsg2")
  val downstreamError3: MtdError        = MtdError("DOWNSTREAM_CODE_DOWNSTREAM", "downstreammsg3")
  val downstreamErrorUnmapped: MtdError = MtdError("DOWNSTREAM_UNMAPPED", "downstreammsg4")

  val error1: MtdError = MtdError("CODE1", "msg1")
  val error2: MtdError = MtdError("CODE2", "msg2")

  val downstreamToMtdErrorMap: PartialFunction[String, MtdError] = {
    case "DOWNSTREAM_CODE1"           => error1
    case "DOWNSTREAM_CODE2"           => error2
    case "DOWNSTREAM_CODE_DOWNSTREAM" => DownstreamError
  }

  val mapToError: ResponseWrapper[D] => Either[ErrorWrapper, ResponseWrapper[D]] = { _: ResponseWrapper[D] =>
    ErrorWrapper(Some(correlationId), error1, None).asLeft[ResponseWrapper[V]]
  }

  override val serviceName = "someService"

  "mapToVendor" when {
    val mapToUpperCase: ResponseWrapper[D] => Either[ErrorWrapper, ResponseWrapper[D]] = { downstreamResponse: ResponseWrapper[D] =>
      Right(ResponseWrapper(downstreamResponse.correlationId, downstreamResponse.responseData.toUpperCase))
    }

    "downstream returns a success outcome" when {
      val goodResponse = ResponseWrapper(correlationId, "downstreamResponse").asRight

      "the specified mapping function returns success" must {
        "use that as the success result" in {
          mapToVendor(ep, downstreamToMtdErrorMap)(mapToUpperCase)(goodResponse) shouldBe
            ResponseWrapper(correlationId, "DOWNSTREAMRESPONSE").asRight
        }
      }

      "the specified mapping function returns a failure" must {
        "use that as the failure result" in {
          mapToVendor(ep, downstreamToMtdErrorMap)(mapToError)(goodResponse) shouldBe
            ErrorWrapper(Some(correlationId), error1, None).asLeft
        }
      }
    }

    "downstream returns an error" when {
      singleErrorBehaveCorrectly(mapToVendor(ep, downstreamToMtdErrorMap)(mapToUpperCase))

      multipleErrorsBehaveCorrectly(mapToVendor(ep, downstreamToMtdErrorMap)(mapToUpperCase))
    }
  }

  "mapToVendorDirect" when {
    "downstream returns a success outcome" when {
      val goodResponse = ResponseWrapper(correlationId, "downstreamResponse").asRight

      "use the downstream content as is" must {
        "use that as the success result" in {
          mapToVendorDirect(ep, downstreamToMtdErrorMap)(goodResponse) shouldBe
            ResponseWrapper(correlationId, "downstreamResponse").asRight
        }
      }
    }

    "downstream returns an error" when {
      singleErrorBehaveCorrectly(mapToVendorDirect(ep, downstreamToMtdErrorMap))

      multipleErrorsBehaveCorrectly(mapToVendorDirect(ep, downstreamToMtdErrorMap))
    }
  }

  private def singleErrorBehaveCorrectly(handler: IfsOutcome[D] => VendorOutcome[D]): Unit = {
    "a single error" must {
      "use the error mapping and return a single mtd error" in {
        val singleErrorResponse = ResponseWrapper(correlationId, SingleError(downstreamError1)).asLeft

        handler(singleErrorResponse) shouldBe
          ErrorWrapper(Some(correlationId), error1, None).asLeft
      }
    }

    "a single unmapped error" must {
      "map to a DownstreamError" in {
        val singleErrorResponse = ResponseWrapper(correlationId, SingleError(downstreamErrorUnmapped)).asLeft

        handler(singleErrorResponse) shouldBe
          ErrorWrapper(Some(correlationId), DownstreamError, None).asLeft
      }
    }

    "an OutboundError" must {
      "return the error inside the OutboundError (regardless of mapping)" in {
        val outboundErrorResponse = ResponseWrapper(correlationId, OutboundError(downstreamError1)).asLeft

        handler(outboundErrorResponse) shouldBe
          ErrorWrapper(Some(correlationId), downstreamError1, None).asLeft
      }
    }
  }

  private def multipleErrorsBehaveCorrectly(handler: IfsOutcome[D] => VendorOutcome[D]): Unit = {
    "multiple errors" must {
      "use the error mapping for each and return multiple mtd errors" in {
        val multipleErrorResponse = ResponseWrapper(correlationId, MultipleErrors(Seq(downstreamError1, downstreamError2))).asLeft

        handler(multipleErrorResponse) shouldBe
          ErrorWrapper(Some(correlationId), BadRequestError, Some(Seq(error1, error2))).asLeft
      }

      "one of the mtd errors is a DownstreamError" must {
        "return a single DownstreamError" in {
          val multipleErrorResponse = ResponseWrapper(correlationId, MultipleErrors(Seq(downstreamError1, downstreamError3))).asLeft

          handler(multipleErrorResponse) shouldBe
            ErrorWrapper(Some(correlationId), DownstreamError, None).asLeft
        }
      }

      "one of the mtd errors is a unmapped" must {
        "return a single DownstreamError" in {
          val multipleErrorResponse = ResponseWrapper(correlationId, MultipleErrors(Seq(downstreamError1, downstreamErrorUnmapped))).asLeft

          handler(multipleErrorResponse) shouldBe
            ErrorWrapper(Some(correlationId), DownstreamError, None).asLeft
        }
      }
    }
  }
}
