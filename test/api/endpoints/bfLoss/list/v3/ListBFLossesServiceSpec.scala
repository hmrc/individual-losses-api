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
import api.services.v3.Outcomes.ListBFLossesOutcome

import scala.concurrent.Future

class ListBFLossesServiceSpec extends ServiceSpec {

  private val nino   = "AA123456A"
  private val lossId = "AAZZ1234567890a"

  private trait Test extends MockBFLossConnector {
    lazy val service = new ListBFLossesService(connector)
  }

  private lazy val request: ListBFLossesRequest = v3.request.ListBFLossesRequest(Nino(nino), None, None, None)

  private val response: ListBFLossesResponse[ListBFLossesItem] =
    ListBFLossesResponse(List(ListBFLossesItem(lossId, "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, "2019-20", "lastModified")))

  private val emptyListResponse: ListBFLossesResponse[ListBFLossesItem] = ListBFLossesResponse(Nil)

  private def downstreamResponse(data: ListBFLossesResponse[ListBFLossesItem]): ResponseWrapper[ListBFLossesResponse[ListBFLossesItem]] =
    ResponseWrapper(correlationId, data)

  "retrieve the list of bf losses" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        private val responseWrapper = downstreamResponse(response)
        MockedBFLossConnector.listBFLosses(request).returns(Future.successful(Right(responseWrapper)))

        private val result: ListBFLossesOutcome = await(service.listBFLosses(request))
        result shouldBe Right(responseWrapper)
      }

      "return a Left(NotFoundError)" when {
        "the connector returns an empty list" in new Test {
          private val responseWrapper = downstreamResponse(emptyListResponse)
          MockedBFLossConnector.listBFLosses(request).returns(Future.successful(Right(responseWrapper)))

          private val result: ListBFLossesOutcome = await(service.listBFLosses(request))
          result shouldBe Left(ErrorWrapper(correlationId, NotFoundError, None))
        }
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        private val someError          = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        private val downstreamResponse = ResponseWrapper(correlationId, OutboundError(someError))
        MockedBFLossConnector.listBFLosses(request).returns(Future.successful(Left(downstreamResponse)))

        private val result: ListBFLossesOutcome = await(service.listBFLosses(request))
        result shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockedBFLossConnector
            .listBFLosses(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.listBFLosses(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_TAXYEAR"           -> TaxYearFormatError,
        "INVALID_INCOMESOURCEID"    -> BusinessIdFormatError,
        "INVALID_INCOMESOURCETYPE"  -> TypeOfLossFormatError,
        "INVALID_CORRELATIONID"     -> InternalError,
        "NOT_FOUND"                 -> NotFoundError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError
      )

      val extraTysErrors = List(
        "INVALID_TAX_YEAR"          -> TaxYearFormatError,
        "INVALID_INCOMESOURCE_ID"   -> BusinessIdFormatError,
        "INVALID_INCOMESOURCE_TYPE" -> TypeOfLossFormatError,
        "INVALID_CORRELATION_ID"    -> InternalError,
        "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError
      )

      (errors ++ extraTysErrors).foreach(args => (serviceError _).tupled(args))
    }
  }

}
