/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.bfLosses.list

import common.errors.TypeOfLossFormatError
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v6.bfLosses.common.domain.TypeOfLoss
import v6.bfLosses.list
import v6.bfLosses.list.ListBFLossesService
import v6.bfLosses.list.def1.model.request.Def1_ListBFLossesRequestData
import v6.bfLosses.list.def1.model.response.{Def1_ListBFLossesResponse, ListBFLossesItem}
import v6.bfLosses.list.model.request.ListBFLossesRequestData
import v6.bfLosses.list.model.response.ListBFLossesResponse

import scala.concurrent.Future

class ListBFLossesServiceSpec extends ServiceSpec {

  private val nino   = "AA123456A"
  private val lossId = "AAZZ1234567890a"

  private val response: ListBFLossesResponse =
    Def1_ListBFLossesResponse(List(ListBFLossesItem(lossId, "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, "2019-20", "lastModified")))

  private val emptyListResponse: Def1_ListBFLossesResponse = Def1_ListBFLossesResponse(Nil)

  "retrieve the list of bf losses" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        private val responseWrapper = downstreamResponse(response)
        MockedListBFLossesConnector.listBFLosses(request()).returns(Future.successful(Right(responseWrapper)))

        private val result = await(service.listBFLosses(request()))
        result shouldBe Right(responseWrapper)
      }

      "return a Left(NotFoundError)" when {
        "the connector returns an empty list" in new Test {
          private val responseWrapper = downstreamResponse(emptyListResponse)
          MockedListBFLossesConnector.listBFLosses(request()).returns(Future.successful(Right(responseWrapper)))

          private val result = await(service.listBFLosses(request()))
          result shouldBe Left(ErrorWrapper(correlationId, NotFoundError, None))
        }
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        private val someError          = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        private val downstreamResponse = ResponseWrapper(correlationId, OutboundError(someError))
        MockedListBFLossesConnector.listBFLosses(request()).returns(Future.successful(Left(downstreamResponse)))

        private val result = await(service.listBFLosses(request()))
        result shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockedListBFLossesConnector
            .listBFLosses(request())
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.listBFLosses(request()))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "NOT_FOUND"                 -> NotFoundError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError,
        "INVALID_TAX_YEAR"          -> TaxYearFormatError,
        "INVALID_INCOMESOURCE_ID"   -> BusinessIdFormatError,
        "INVALID_INCOMESOURCE_TYPE" -> TypeOfLossFormatError,
        "INVALID_CORRELATION_ID"    -> InternalError,
        "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError
      )

      errors.foreach(args => (serviceError _).tupled(args))
    }
  }

  private trait Test extends list.MockListBFLossesConnector {
    lazy val service = new ListBFLossesService(connector)
  }

  private def request(taxYear: TaxYear = TaxYear.fromMtd("2020-21")): ListBFLossesRequestData =
    Def1_ListBFLossesRequestData(Nino(nino), taxYear, None, None)

  private def downstreamResponse(data: ListBFLossesResponse): ResponseWrapper[ListBFLossesResponse] =
    ResponseWrapper(correlationId, data)

}
