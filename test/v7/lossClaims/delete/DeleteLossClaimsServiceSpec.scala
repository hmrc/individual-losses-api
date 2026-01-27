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

package v7.lossClaims.delete

import common.errors.RuleOutsideAmendmentWindow
import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v7.lossClaims.delete.model.request.DeleteLossClaimsRequestData

import scala.concurrent.Future

class DeleteLossClaimsServiceSpec extends ServiceSpec {

  private val nino: String       = "AA123456A"
  private val businessId: String = "XAIS12345678910"
  private val taxYear: String    = "2019-20"

  trait Test extends MockDeleteLossClaimsConnector {
    lazy val service = new DeleteLossClaimsService(connector)
  }

  lazy val request: DeleteLossClaimsRequestData = DeleteLossClaimsRequestData(Nino(nino), BusinessId(businessId), TaxYear.fromMtd(taxYear))

  "Delete Loss Claims" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val downstreamResponse: ResponseWrapper[Unit] = ResponseWrapper(correlationId, ())
        val expected: ResponseWrapper[Unit]           = ResponseWrapper(correlationId, ())
        MockDeleteLossClaimsConnector.deleteLossClaims(request).returns(Future.successful(Right(downstreamResponse)))

        await(service.deleteLossClaimsService(request)) shouldBe Right(expected)
      }
    }

    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError: MtdError                                = MtdError("SOME_CODE", "some message", BAD_REQUEST)
        val downstreamResponse: ResponseWrapper[OutboundError] = ResponseWrapper(correlationId, OutboundError(someError))
        MockDeleteLossClaimsConnector.deleteLossClaims(request).returns(Future.successful(Left(downstreamResponse)))

        await(service.deleteLossClaimsService(request)) shouldBe Left(ErrorWrapper(correlationId, someError, None))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockDeleteLossClaimsConnector
            .deleteLossClaims(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.deleteLossClaimsService(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val commonErrors: Seq[(String, MtdError)] = List(
        "SERVER_ERROR"        -> InternalError,
        "SERVICE_UNAVAILABLE" -> InternalError
      )

      val itsdErrors: Seq[(String, MtdError)] = List(
        "1215" -> NinoFormatError,
        "1117" -> TaxYearFormatError,
        "1216" -> InternalError,
        "1007" -> BusinessIdFormatError,
        "4200" -> RuleOutsideAmendmentWindow,
        "5000" -> RuleTaxYearNotSupportedError,
        "5010" -> NotFoundError
      )

      (commonErrors ++ itsdErrors).foreach(serviceError.tupled)
    }

  }

}
