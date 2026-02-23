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

package v7.lossesAndClaims.createAmend

import common.errors.RuleOutsideAmendmentWindow
import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v7.lossesAndClaims.createAmend.fixtures.CreateAmendLossesAndClaimsFixtures.requestBodyModel
import v7.lossesAndClaims.createAmend.request.*

import scala.concurrent.Future

class CreateAmendLossesAndClaimsServiceSpec extends ServiceSpec {

  private val nino: String       = "AA123456A"
  private val businessId: String = "XAIS12345678910"
  private val taxYear: String    = "2026-27"

  private val request: CreateAmendLossesAndClaimsRequestData = CreateAmendLossesAndClaimsRequestData(
    nino = Nino(nino),
    businessId = BusinessId(businessId),
    taxYear = TaxYear.fromMtd(taxYear),
    createAmendLossesAndClaimsRequestBody = requestBodyModel
  )

  private trait Test extends MockCreateAmendLossesAndClaimsConnector {
    lazy val service = new CreateAmendLossesAndClaimsService(connector)
  }

  "createAmendLossesAndClaims" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        MockCreateAmendLossesAndClaimsConnector
          .createAmendLossesAndClaims(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.createAmendLossesAndClaims(request)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockCreateAmendLossesAndClaimsConnector
            .createAmendLossesAndClaims(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.createAmendLossesAndClaims(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))

        }

      val errors: Seq[(String, MtdError)] = List(
        "1215" -> NinoFormatError,
        "1117" -> TaxYearFormatError,
        "1216" -> InternalError,
        "1000" -> InternalError,
        "1007" -> BusinessIdFormatError,
        "1115" -> RuleTaxYearNotEndedError,
        "1253" -> RuleMissingPreferenceOrderError,
        "1254" -> RuleCarryForwardAndTerminalLossNotAllowedError,
        "1262" -> RuleCarryBackClaimError,
        "4200" -> RuleOutsideAmendmentWindow,
        "5000" -> InternalError,
        "5010" -> NotFoundError
      )

      errors.foreach(serviceError.tupled)
    }
  }

}
