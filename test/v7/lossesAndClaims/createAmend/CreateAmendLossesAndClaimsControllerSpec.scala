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

import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.test.Helpers.PUT
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import shared.models.domain.{BusinessId, TaxYear}
import shared.models.errors.{BusinessIdFormatError, ErrorWrapper, NinoFormatError}
import shared.models.outcomes.ResponseWrapper
import v7.lossesAndClaims.createAmend.fixtures.CreateAmendLossesAndClaimsFixtures.{requestBodyJson, requestBodyModel}
import v7.lossesAndClaims.createAmend.request.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateAmendLossesAndClaimsControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockCreateAmendLossesAndClaimsService
    with MockCreateAmendLossesAndClaimsValidatorFactory {

  private val businessId: String = "XAIS12345678910"
  private val taxYear: String    = "2026-27"

  private val requestData: CreateAmendLossesAndClaimsRequestData = CreateAmendLossesAndClaimsRequestData(
    nino = parsedNino,
    businessId = BusinessId(businessId),
    taxYear = TaxYear.fromMtd(taxYear),
    createAmendLossesAndClaimsRequestBody = requestBodyModel
  )

  "createAmend" should {
    "return NO_CONTENT" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockCreateAmendLossesAndClaimsService
          .createAmend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(expectedStatus = NO_CONTENT, maybeAuditRequestBody = Some(requestBodyJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTestWithAudit(NinoFormatError, Some(requestBodyJson))
      }

      "service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockCreateAmendLossesAndClaimsService
          .createAmend(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, BusinessIdFormatError))))

        runErrorTestWithAudit(BusinessIdFormatError, Some(requestBodyJson))
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

    protected def callController(): Future[Result] = controller.createAmend(
      nino = validNino,
      businessId = businessId,
      taxYear = taxYear
    )(fakeRequest.withBody(requestBodyJson).withMethod(PUT))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateAmendLossesAndClaims",
        transactionName = "create-or-amend-losses-and-claims",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = apiVersion.name,
          params = Map("nino" -> validNino, "businessId" -> businessId, "taxYear" -> taxYear),
          requestBody = Some(requestBodyJson),
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns true

  }

}
