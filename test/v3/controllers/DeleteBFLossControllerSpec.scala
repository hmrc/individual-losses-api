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

package v3.controllers

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.models.ResponseWrapper
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.Nino
import api.models.errors._
import api.services.MockAuditService
import play.api.libs.json.JsValue
import play.api.mvc.Result
import v3.controllers.validators.MockDeleteBFLossValidatorFactory
import v3.models.domain.bfLoss.LossId
import v3.models.request.deleteBFLosses.DeleteBFLossRequestData
import v3.services.MockDeleteBFLossService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteBFLossControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockDeleteBFLossService
    with MockDeleteBFLossValidatorFactory
    with MockAuditService {

  private val lossId      = "AAZZ1234567890a"
  private val requestData = DeleteBFLossRequestData(Nino(nino), LossId(lossId))

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

    protected def callController(): Future[Result] = controller.delete(nino, lossId)(fakeRequest)

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "DeleteBroughtForwardLoss",
        transactionName = "delete-brought-forward-loss",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = "3.0",
          params = Map("nino" -> nino, "lossId" -> lossId),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

  }

}
