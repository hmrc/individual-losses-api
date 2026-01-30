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

import cats.implicits.catsSyntaxValidatedId
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.domain.{BusinessId, TaxYear}
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import shared.services.MockAuditService
import v7.lossesAndClaims.retrieve.model.request.RetrieveLossesAndClaimsRequestData
import v7.lossesAndClaims.retrieve.model.response.*
import v7.lossesAndClaims.retrieve.model.response.PreferenceOrderEnum.`carry-back`

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveLossesAndClaimsControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with MockRetrieveLossesAndClaimsService
    with MockRetrieveLossesAndClaimsValidatorFactory
    with MockAuditService {

  private val businessId  = "X0IS12345678901"
  private val taxYear     = "2026-27"
  private val requestData = RetrieveLossesAndClaimsRequestData(parsedNino, BusinessId(businessId), TaxYear.fromMtd(taxYear))

  private val retrieveResponse: RetrieveLossesAndClaimsResponse = RetrieveLossesAndClaimsResponse(
    "2026-08-24T14:15:22.544Z",
    Some(
      Claims(
        Some(
          CarryBack(
            Some(5000.99),
            Some(5000.99),
            Some(5000.99)
          )),
        Some(
          CarrySideways(
            Some(5000.99)
          )),
        Some(
          PreferenceOrder(
            Some(`carry-back`)
          )),
        Some(
          CarryForward(
            Some(5000.99),
            Some(5000.99)
          ))
      )),
    Some(
      Losses(
        Some(5000.99)
      ))
  )

  private val mtdResponseJson: JsValue = Json.parse(s"""
       |{
       |  "submittedOn": "2026-08-24T14:15:22.544Z",
       |  "claims": {
       |    "carryBack": {
       |      "previousYearGeneralIncome": 5000.99,
       |      "earlyYearLosses": 5000.99,
       |      "terminalLosses": 5000.99
       |    },
       |    "carrySideways": {
       |      "currentYearGeneralIncome": 5000.99
       |    },
       |    "preferenceOrder": {
       |      "applyFirst": "carry-back"
       |    },
       |    "carryForward": {
       |      "currentYearLosses": 5000.99,
       |      "previousYearsLosses": 5000.99
       |    }
       |  },
       |  "losses": {
       |    "broughtForwardLosses": 5000.99
       |  }
       |}
          """.stripMargin)

  "retrieve" should {
    "return OK" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveLossesAndClaimsService
          .retrieve(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveResponse))))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(BusinessIdFormatError))
        runErrorTest(BusinessIdFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveLossesAndClaimsService
          .retrieve(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError, None))))

        runErrorTest(RuleTaxYearNotSupportedError)
      }
    }
  }

  private trait Test extends ControllerTest {

    val controller: RetrieveLossesAndClaimsController = new RetrieveLossesAndClaimsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockRetrieveLossesAndClaimsService,
      validatorFactory = mockRetrieveLossesAndClaimsValidatorFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.retrieve(validNino, businessId, taxYear)(fakeRequest)

    MockedSharedAppConfig.deprecationFor(Version9).returns(NotDeprecated.valid).anyNumberOfTimes()

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

  }

}
