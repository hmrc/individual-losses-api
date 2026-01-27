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

package v7.lossClaim.delete

import cats.implicits.catsSyntaxValidatedId
import common.errors.RuleDeleteAfterFinalDeclarationError
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.audit.*
import shared.models.domain.{BusinessId, TaxYear}
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import shared.services.MockAuditService
import v7.lossClaim.delete.model.request.DeleteLossClaimRequestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteLossClaimControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with MockDeleteLossClaimService
    with MockDeleteLossClaimValidatorFactory
    with MockAuditService {

  private val businessId  = "AAZZ1234567890a"
  private val taxYear     = "2019-20"
  private val requestData = DeleteLossClaimRequestData(parsedNino, BusinessId(businessId), TaxYear.fromMtd(taxYear))

  "delete" should {
    "return NoContent" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteLossClaimService
          .delete(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(expectedStatus = NO_CONTENT, maybeExpectedResponseBody = None)
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))
        runErrorTestWithAudit(NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteLossClaimService
          .delete(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleDeleteAfterFinalDeclarationError, None))))

        runErrorTestWithAudit(RuleDeleteAfterFinalDeclarationError)
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller: DeleteLossClaimController = new DeleteLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockDeleteLossClaimService,
      validatorFactory = mockDeleteLossClaimValidatorFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.delete(validNino, businessId, taxYear)(fakeRequest)

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "DeleteLossClaim",
        transactionName = "delete-loss-claim",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = Version9.name,
          params = Map("nino" -> validNino, "businessId" -> businessId, "taxYear" -> taxYear),
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
