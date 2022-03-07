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

package v2.services

import api.models.errors._
import api.models.outcomes.ResponseWrapper
import cats.syntax.either._
import support.UnitSpec
import v2.connectors.DesOutcome

class DesServiceSupportSpec extends UnitSpec with DesServiceSupport {

  type D = String
  type V = String

  val ep            = "someEndpoint"
  val correlationId = "correllationId"

  val desError1        = MtdError("DES_CODE1", "desmsg1")
  val desError2        = MtdError("DES_CODE2", "desmsg2")
  val desError3        = MtdError("DES_CODE_DOWNSTREAM", "desmsg3")
  val desErrorUnmapped = MtdError("DES_UNMAPPED", "desmsg4")

  val error1 = MtdError("CODE1", "msg1")
  val error2 = MtdError("CODE2", "msg2")

  val desToMtdErrorMap: PartialFunction[String, MtdError] = {
    case "DES_CODE1"           => error1
    case "DES_CODE2"           => error2
    case "DES_CODE_DOWNSTREAM" => StandardDownstreamError
  }

  val mapToError: ResponseWrapper[D] => Either[ErrorWrapper, ResponseWrapper[D]] = { _: ResponseWrapper[D] =>
    ErrorWrapper(Some(correlationId), error1, None).asLeft[ResponseWrapper[V]]
  }

  override val serviceName = "someService"

  "mapToVendor" when {
    val mapToUpperCase: ResponseWrapper[D] => Either[ErrorWrapper, ResponseWrapper[D]] = { desResponse: ResponseWrapper[D] =>
      Right(ResponseWrapper(desResponse.correlationId, desResponse.responseData.toUpperCase))
    }

    "des returns a success outcome" when {
      val goodResponse = ResponseWrapper(correlationId, "desresponse").asRight

      "the specified mapping function returns success" must {
        "use that as the success result" in {
          mapToVendor(ep, desToMtdErrorMap)(mapToUpperCase)(goodResponse) shouldBe
            ResponseWrapper(correlationId, "DESRESPONSE").asRight
        }
      }

      "the specified mapping function returns a failure" must {
        "use that as the failure result" in {
          mapToVendor(ep, desToMtdErrorMap)(mapToError)(goodResponse) shouldBe
            ErrorWrapper(Some(correlationId), error1, None).asLeft
        }
      }
    }

    "des returns an error" when {
      singleErrorBehaveCorrectly(mapToVendor(ep, desToMtdErrorMap)(mapToUpperCase))

      multipleErrorsBehaveCorrectly(mapToVendor(ep, desToMtdErrorMap)(mapToUpperCase))
    }
  }

  "mapToVendorDirect" when {
    "des returns a success outcome" when {
      val goodResponse = ResponseWrapper(correlationId, "desresponse").asRight

      "use the des content as is" must {
        "use that as the success result" in {
          mapToVendorDirect(ep, desToMtdErrorMap)(goodResponse) shouldBe
            ResponseWrapper(correlationId, "desresponse").asRight
        }
      }
    }

    "des returns an error" when {
      singleErrorBehaveCorrectly(mapToVendorDirect(ep, desToMtdErrorMap))

      multipleErrorsBehaveCorrectly(mapToVendorDirect(ep, desToMtdErrorMap))
    }
  }

  private def singleErrorBehaveCorrectly(handler: DesOutcome[D] => VendorOutcome[D]): Unit = {
    "a single error" must {
      "use the error mapping and return a single mtd error" in {
        val singleErrorResponse = ResponseWrapper(correlationId, SingleError(desError1)).asLeft

        handler(singleErrorResponse) shouldBe
          ErrorWrapper(Some(correlationId), error1, None).asLeft
      }
    }

    "a single unmapped error" must {
      "map to a DownstreamError" in {
        val singleErrorResponse = ResponseWrapper(correlationId, SingleError(desErrorUnmapped)).asLeft

        handler(singleErrorResponse) shouldBe
          ErrorWrapper(Some(correlationId), StandardDownstreamError, None).asLeft
      }
    }

    "an OutboundError" must {
      "return the error inside the OutboundError (regardless of mapping)" in {
        val outboundErrorResponse = ResponseWrapper(correlationId, OutboundError(desError1)).asLeft

        handler(outboundErrorResponse) shouldBe
          ErrorWrapper(Some(correlationId), desError1, None).asLeft
      }
    }
  }

  private def multipleErrorsBehaveCorrectly(handler: DesOutcome[D] => VendorOutcome[D]): Unit = {
    "multiple errors" must {
      "use the error mapping for each and return multiple mtd errors" in {
        val multipleErrorResponse = ResponseWrapper(correlationId, MultipleErrors(Seq(desError1, desError2))).asLeft

        handler(multipleErrorResponse) shouldBe
          ErrorWrapper(Some(correlationId), BadRequestError, Some(Seq(error1, error2))).asLeft
      }

      "one of the mtd errors is a DownstreamError" must {
        "return a single DownstreamError" in {
          val multipleErrorResponse = ResponseWrapper(correlationId, MultipleErrors(Seq(desError1, desError3))).asLeft

          handler(multipleErrorResponse) shouldBe
            ErrorWrapper(Some(correlationId), StandardDownstreamError, None).asLeft
        }
      }

      "one of the mtd errors is a unmapped" must {
        "return a single DownstreamError" in {
          val multipleErrorResponse = ResponseWrapper(correlationId, MultipleErrors(Seq(desError1, desErrorUnmapped))).asLeft

          handler(multipleErrorResponse) shouldBe
            ErrorWrapper(Some(correlationId), StandardDownstreamError, None).asLeft
        }
      }
    }
  }
}
