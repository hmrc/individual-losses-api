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

package api.endpoints.bfLoss.list.v3

import api.endpoints.bfLoss.connector.v3.MockBFLossConnector
import api.endpoints.bfLoss.domain.v3.TypeOfLoss
import api.endpoints.bfLoss.list.v3
import api.endpoints.bfLoss.list.v3.request.ListBFLossesRequest
import api.endpoints.bfLoss.list.v3.response.{ ListBFLossesItem, ListBFLossesResponse }
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._
import api.services.ServiceSpec

import scala.concurrent.Future

class ListBFLossesServiceSpec extends ServiceSpec {

  val nino: String                            = "AA123456A"
  val lossId: String                          = "AAZZ1234567890a"
  override implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  trait Test extends MockBFLossConnector {
    lazy val service = new ListBFLossesService(connector)
  }

  lazy val request: ListBFLossesRequest = v3.request.ListBFLossesRequest(Nino(nino), None, None, None)

  val response: ListBFLossesResponse[ListBFLossesItem] =
    ListBFLossesResponse(Seq(ListBFLossesItem(lossId, "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, "2019-20", "lastModified")))

  "retrieve the list of bf losses" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[ListBFLossesResponse[ListBFLossesItem]] =
          ResponseWrapper(correlationId, response)
        MockedBFLossConnector.listBFLosses(request).returns(Future.successful(Right(downstreamResponse)))

        await(service.listBFLosses(request)) shouldBe Right(downstreamResponse)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message")
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedBFLossConnector.listBFLosses(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.listBFLosses(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "return a downstream error" when {
      "the connector call returns a single downstream error" in new Test {
        val downstreamResponse: ResponseWrapper[SingleError] = ResponseWrapper(correlationId, SingleError(StandardDownstreamError))
        val expected: ErrorWrapper                           = ErrorWrapper(correlationId, StandardDownstreamError, None)
        MockedBFLossConnector.listBFLosses(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.listBFLosses(request)) shouldBe Left(expected)
      }

      "the connector call returns multiple errors including a downstream error" in new Test {
        val downstreamResponse: ResponseWrapper[MultipleErrors] =
          ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, StandardDownstreamError)))
        val expected: ErrorWrapper = ErrorWrapper(correlationId, StandardDownstreamError, None)
        MockedBFLossConnector.listBFLosses(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.listBFLosses(request)) shouldBe Left(expected)
      }
    }

    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAXYEAR"           -> TaxYearFormatError,
      "INVALID_INCOMESOURCEID"    -> BusinessIdFormatError,
      "INVALID_INCOMESOURCETYPE"  -> TypeOfLossFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "SERVER_ERROR"              -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
    ).foreach {
      case (k, v) =>
        s"return a ${v.code} error" when {
          s"the connector call returns $k" in new Test {
            MockedBFLossConnector
              .listBFLosses(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, SingleError(MtdError(k, "doesn't matter", v.httpStatus))))))

            await(service.listBFLosses(request)) shouldBe Left(ErrorWrapper(correlationId, v, None))
          }
        }
    }
  }
}
