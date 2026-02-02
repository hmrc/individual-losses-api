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
import v7.lossesAndClaims.common.PreferenceOrderEnum.`carry-back`
import v7.lossesAndClaims.common.{Losses, PreferenceOrder}
import v7.lossesAndClaims.createAmend.request.*

import scala.concurrent.Future

class CreateAmendLossesAndClaimsServiceSpec extends ServiceSpec {

  private val nino: String       = "AA123456A"
  private val businessId: String = "XAIS12345678910"
  private val taxYear: String    = "2019-20"

  val createAmendLossesAndClaimsRequestBody: CreateAmendLossesAndClaimsRequestBody = CreateAmendLossesAndClaimsRequestBody(
    Option(
      Claims(
        Option(
          CarryBack(
            Option(5000.99),
            Option(5000.99),
            Option(5000.99)
          )),
        Option(
          CarrySideways(
            Option(5000.99)
          )),
        Option(
          PreferenceOrder(
            Option(`carry-back`)
          )),
        Option(
          CarryForward(
            Option(5000.99),
            Option(5000.99)
          ))
      )),
    Option(
      Losses(
        Option(5000.99)
      ))
  )

  trait Test extends MockCreateAmendLossesAndClaimsConnector {
    lazy val service = new CreateAmendLossesAndClaimsService(connector)
  }

  "createAmend Losses and Claims" when {
    lazy val request =
      CreateAmendLossesAndClaimsRequestData(Nino(nino), BusinessId(businessId), TaxYear.fromMtd(taxYear), createAmendLossesAndClaimsRequestBody)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockCreateAndAmendLossesAndClaimsConnector
          .createAndAmendLossesAndClaims(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.createAmendLossesAndClaims(request)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {
          MockCreateAndAmendLossesAndClaimsConnector
            .createAndAmendLossesAndClaims(request)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          private val result = await(service.createAmendLossesAndClaims(request))
          result shouldBe Left(ErrorWrapper(correlationId, error))

        }

      val itsdError: Seq[(String, MtdError)] = List(
        "1215" -> NinoFormatError,
        "1117" -> TaxYearFormatError,
        "1216" -> InternalError,
        "1000" -> InternalError,
        "1007" -> BusinessIdFormatError,
        "1115" -> RuleTaxYearNotEndedError,
        "1253" -> RuleMissingPreferenceOrder,
        "1254" -> RuleCarryForwardAndTerminalLossNotAllowed,
        "1262" -> RuleCarryBackClaim,
        "4200" -> RuleOutsideAmendmentWindow,
        "5000" -> RuleTaxYearNotSupportedError,
        "5010" -> NotFoundError
      )

      itsdError.foreach(serviceError.tupled)
    }
  }

}
