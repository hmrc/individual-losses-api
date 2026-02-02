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

import cats.implicits.catsSyntaxValidatedId
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.hateoas.MockHateoasFactory
import shared.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import shared.models.domain.{BusinessId, TaxYear}
import shared.models.errors.{BusinessIdFormatError, ErrorWrapper, NinoFormatError}
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import v7.lossesAndClaims.commons.PreferenceOrderEnum.`carry-back`
import v7.lossesAndClaims.commons.{Losses, PreferenceOrder}
import v7.lossesAndClaims.createAmend.request.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateAmendLossesAndClaimsControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with MockCreateAmendLossesAndClaimsService
    with MockCreateAmendLossesAndClaimsValidatorFactory
    with MockHateoasFactory {

  private val nino: String       = "AA123456A"
  private val businessId: String = "XAIS12345678910"
  private val taxYear: String    = "2019-20"

  private val requestBody: CreateAmendLossesAndClaimsRequestBody = CreateAmendLossesAndClaimsRequestBody(
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

  private val requestJson: JsValue = Json.parse(s"""
       |{
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

  private val requestData = CreateAmendLossesAndClaimsRequestData(parsedNino, BusinessId(businessId), TaxYear.fromMtd(taxYear), requestBody)

  "createAmend" should {
    "return NO_CONTENT" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockCreateAmendLossesAndClaimsService
          .createAmend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(expectedStatus = NO_CONTENT, maybeAuditRequestBody = Option(requestJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTestWithAudit(NinoFormatError, Option(requestJson))
      }

      "service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockCreateAmendLossesAndClaimsService
          .createAmend(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, BusinessIdFormatError))))

        runErrorTestWithAudit(BusinessIdFormatError, Option(requestJson))
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller: CreateAmendLossesAndClaimsController = new CreateAmendLossesAndClaimsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockCreateAmendLossesAndClaimsService,
      validatorFactory = mockCreateAmendLossesAndClaimsValidatorFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.createAmend(nino, businessId, taxYear)(fakePostRequest(requestJson))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateAmendLossesAndClaims",
        transactionName = "create-and-amend-losses-and-claims",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = Version9.name,
          params = Map("nino" -> nino, "businessId" -> businessId, "taxYear" -> taxYear),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

    MockedSharedAppConfig.deprecationFor(Version9).returns(NotDeprecated.valid).anyNumberOfTimes()

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

  }

}
