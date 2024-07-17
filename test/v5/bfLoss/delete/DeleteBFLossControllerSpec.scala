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

package v5.bfLoss.delete

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.MockAuditService
import config.MockAppConfig
import play.api.libs.json.JsValue
import play.api.mvc.Result
import routing.Version5
import v5.bfLoss.delete
import v5.bfLossClaims.delete.DeleteBFLossController
import v5.bfLossClaims.delete.def1.model.request.Def1_DeleteBFLossRequestData
import v5.bfLossClaims.delete.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteBFLossControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAppConfig
    with delete.MockDeleteBFLossService
    with delete.MockDeleteBFLossValidatorFactory
    with MockAuditService {

  private val lossId      = "AAZZ1234567890a"
  private val requestData = Def1_DeleteBFLossRequestData(parsedNino, LossId(lossId))

  "delete" should {
    "return NoContent" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteBFLossService
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

        MockDeleteBFLossService
          .delete(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleDeleteAfterFinalDeclarationError, None))))

        runErrorTestWithAudit(RuleDeleteAfterFinalDeclarationError)
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking {

    private val controller = new DeleteBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockDeleteBFLossService,
      validatorFactory = mockDeleteBFLossValidatorFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.delete(validNino, lossId)(fakeRequest)

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "DeleteBroughtForwardLoss",
        transactionName = "delete-brought-forward-loss",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = "4.0",
          params = Map("nino" -> validNino, "lossId" -> lossId),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

    MockedAppConfig.isApiDeprecated(Version5) returns false
  }

}
