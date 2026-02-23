/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.retrieve

import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v7.lossesAndClaims.retrieve.fixtures.RetrieveLossesAndClaimsFixtures.responseBodyModel
import v7.lossesAndClaims.retrieve.model.request.RetrieveLossesAndClaimsRequestData
import v7.lossesAndClaims.retrieve.model.response.RetrieveLossesAndClaimsResponse

import scala.concurrent.Future

class RetrieveLossesAndClaimsServiceSpec extends ServiceSpec {

  private val nino: String       = "AA123456A"
  private val businessId: String = "XAIS12345678910"
  private val taxYear: String    = "2026-27"

  private val request: RetrieveLossesAndClaimsRequestData = RetrieveLossesAndClaimsRequestData(
    nino = Nino(nino),
    businessId = BusinessId(businessId),
    taxYear = TaxYear.fromMtd(taxYear)
  )

  private trait Test extends MockRetrieveLossesAndClaimsConnector {
    lazy val service = new RetrieveLossesAndClaimsService(connector)
  }

  "retrieveLossesAndClaims" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[RetrieveLossesAndClaimsResponse] = ResponseWrapper(correlationId, responseBodyModel)
        MockRetrieveLossesAndClaimsConnector
          .retrieveLossesAndClaims(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseBodyModel))))

        await(service.retrieveLossesAndClaims(request)) shouldBe Right(downstreamResponse)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockRetrieveLossesAndClaimsConnector.retrieveLossesAndClaims(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.retrieveLossesAndClaims(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockRetrieveLossesAndClaimsConnector
            .retrieveLossesAndClaims(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.retrieveLossesAndClaims(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors: Seq[(String, MtdError)] = List(
        "1215" -> NinoFormatError,
        "1117" -> TaxYearFormatError,
        "1216" -> InternalError,
        "1007" -> BusinessIdFormatError,
        "5000" -> InternalError,
        "5010" -> NotFoundError
      )

      errors.foreach(serviceError.tupled)
    }

  }

}
