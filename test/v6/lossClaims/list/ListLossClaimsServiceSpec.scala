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

package v6.lossClaims.list

import common.errors.{TaxYearClaimedForFormatError, TypeOfClaimFormatError, TypeOfLossFormatError}
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v6.lossClaims.fixtures.ListLossClaimsFixtures.singleClaimResponseModel
import v6.lossClaims.list.def1.request.Def1_ListLossClaimsRequestData
import v6.lossClaims.list.def1.response.Def1_ListLossClaimsResponse
import v6.lossClaims.list.model.request.ListLossClaimsRequestData
import v6.lossClaims.list.model.response.ListLossClaimsResponse

import scala.concurrent.Future

class ListLossClaimsServiceSpec extends ServiceSpec {

  val nino    = "AA123456A"
  val claimId = "AAZZ1234567890a"

  trait Test extends MockListLossClaimsConnector {
    lazy val service = new ListLossClaimsService(connector)

    def request(taxYear: TaxYear = TaxYear.fromMtd("2020-21")): ListLossClaimsRequestData =
      Def1_ListLossClaimsRequestData(Nino(nino), taxYear, None, None, None)

  }

  "retrieve the list of bf losses" should {
    "return a Right" when {

      "the connector call is successful" in new Test {
        private val taxYear = TaxYear.fromMtd("2023-24")
        private val downstreamResponse: ResponseWrapper[ListLossClaimsResponse] =
          ResponseWrapper(
            correlationId,
            singleClaimResponseModel("2023-24")
          )
        MockedListLossClaimsConnector.listLossClaims(request(taxYear)).returns(Future.successful(Right(downstreamResponse)))

        private val result = await(service.listLossClaims(request(taxYear)))
        result shouldBe Right(downstreamResponse)
      }
    }
    "return a Left(NotFoundError)" when {
      "the connector returns an empty list" in new Test {
        private val taxYear = TaxYear.fromMtd("2023-24")
        private val downstreamResponse: ResponseWrapper[ListLossClaimsResponse] =
          ResponseWrapper(
            correlationId,
            Def1_ListLossClaimsResponse(Nil)
          )
        MockedListLossClaimsConnector.listLossClaims(request(taxYear)).returns(Future.successful(Right(downstreamResponse)))

        private val result = await(service.listLossClaims(request(taxYear)))
        result shouldBe Left(ErrorWrapper(correlationId, NotFoundError, None))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        private val someError                                          = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        private val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedListLossClaimsConnector.listLossClaims(request()).returns(Future.successful(Left(downstreamResponse)))

        private val result = await(service.listLossClaims(request()))
        result shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockedListLossClaimsConnector
            .listLossClaims(request())
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.listLossClaims(request()))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_CLAIM_TYPE"        -> TypeOfClaimFormatError,
        "NOT_FOUND"                 -> NotFoundError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError,
        "INVALID_CORRELATION_ID"    -> InternalError,
        "INVALID_TAX_YEAR"          -> TaxYearClaimedForFormatError,
        "INVALID_INCOMESOURCE_ID"   -> BusinessIdFormatError,
        "INVALID_INCOMESOURCE_TYPE" -> TypeOfLossFormatError,
        "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError
      )

      errors.foreach(serviceError.tupled)
    }
  }

}
