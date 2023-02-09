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

package api.endpoints.lossClaim.list.v3

import api.endpoints.lossClaim.list.v3.connector.MockListLossClaimsConnector
import api.endpoints.lossClaim.list.v3.request.ListLossClaimsRequest
import api.endpoints.lossClaim.list.v3.response.{ ListLossClaimsItem, ListLossClaimsResponse }
import api.fixtures.ListLossClaimsFixtures.{ multipleListLossClaimsResponseModel, singleListLossClaimsResponseModel }
import api.models.ResponseWrapper
import api.models.domain.{ Nino, TaxYear }
import api.models.errors._
import api.services.ServiceSpec
import api.services.v3.Outcomes.ListLossClaimsOutcome

import scala.concurrent.Future

class ListLossClaimsServiceSpec extends ServiceSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  trait Test extends MockListLossClaimsConnector {
    lazy val service                        = new ListLossClaimsService(connector)
    lazy val request: ListLossClaimsRequest = ListLossClaimsRequest(Nino(nino), None, None, None, None)
  }

  "retrieve the list of bf losses" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[ListLossClaimsResponse[ListLossClaimsItem]] =
          ResponseWrapper(correlationId, multipleListLossClaimsResponseModel)
        MockedListLossClaimsConnector.listLossClaims(request).returns(Future.successful(Right(downstreamResponse)))

        private val result: ListLossClaimsOutcome = await(service.listLossClaims(request))
        result shouldBe Right(downstreamResponse)
      }

      "the connector call is successful for a TYS tax year" in new Test {
        private val tysTaxYear                           = Some(TaxYear.fromMtd("2023-24"))
        override lazy val request: ListLossClaimsRequest = ListLossClaimsRequest(Nino(nino), tysTaxYear, None, None, None)
        private val downstreamResponse: ResponseWrapper[ListLossClaimsResponse[ListLossClaimsItem]] =
          ResponseWrapper(
            correlationId,
            singleListLossClaimsResponseModel("2023-24")
          )
        MockedListLossClaimsConnector.listLossClaims(request).returns(Future.successful(Right(downstreamResponse)))

        private val result: ListLossClaimsOutcome = await(service.listLossClaims(request))
        result shouldBe Right(downstreamResponse)
      }
    }

    "return a Left(NotFoundError)" when {
      "the connector returns an empty list" in new Test {
        private val tysTaxYear                           = Some(TaxYear.fromMtd("2023-24"))
        override lazy val request: ListLossClaimsRequest = ListLossClaimsRequest(Nino(nino), tysTaxYear, None, None, None)
        private val downstreamResponse: ResponseWrapper[ListLossClaimsResponse[ListLossClaimsItem]] =
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(Nil)
          )
        MockedListLossClaimsConnector.listLossClaims(request).returns(Future.successful(Right(downstreamResponse)))

        private val result: ListLossClaimsOutcome = await(service.listLossClaims(request))
        result shouldBe Left(ErrorWrapper(correlationId, NotFoundError, None))
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        private val someError                                          = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        private val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockedListLossClaimsConnector.listLossClaims(request).returns(Future.successful(Left(downstreamResponse)))

        private val result: ListLossClaimsOutcome = await(service.listLossClaims(request))
        result shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockedListLossClaimsConnector
            .listLossClaims(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.listLossClaims(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_TAXYEAR"           -> TaxYearFormatError,
        "INVALID_INCOMESOURCEID"    -> BusinessIdFormatError,
        "INVALID_INCOMESOURCETYPE"  -> TypeOfLossFormatError,
        "INVALID_CLAIM_TYPE"        -> TypeOfClaimFormatError,
        "NOT_FOUND"                 -> NotFoundError,
        "INVALID_CORRELATIONID"     -> InternalError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError
      )

      val extraTysErrors = List(
        "INVALID_CORRELATION_ID"    -> InternalError,
        "INVALID_TAX_YEAR"          -> TaxYearFormatError,
        "INVALID_INCOMESOURCE_ID"   -> BusinessIdFormatError,
        "INVALID_INCOMESOURCE_TYPE" -> TypeOfLossFormatError,
        "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError
      )

      (errors ++ extraTysErrors).foreach(args => (serviceError _).tupled(args))
    }
  }

}
