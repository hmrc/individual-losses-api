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
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetailOld}
import api.models.domain.Nino
import api.models.errors._
import play.api.libs.json.JsValue
import play.api.mvc.Result
import v3.controllers.requestParsers.MockDeleteLossClaimRequestParser
import v3.models.request.deleteLossClaim.{DeleteLossClaimRawData, DeleteLossClaimRequest}
import v3.services.MockDeleteLossClaimService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteLossClaimControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockDeleteLossClaimService
    with MockDeleteLossClaimRequestParser {

  private val claimId = "AAZZ1234567890a"
  private val rawData = DeleteLossClaimRawData(nino, claimId)
  private val request = DeleteLossClaimRequest(Nino(nino), claimId)

  "delete" should {
    "return NO_CONTENT" when {
      "the request is valid" in new Test {
        MockDeleteLossClaimRequestParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockDeleteLossClaimService
          .delete(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(expectedStatus = NO_CONTENT, maybeExpectedResponseBody = None)
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        MockDeleteLossClaimRequestParser
          .parseRequest(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

        runErrorTestWithAudit(NinoFormatError)
      }

      "the service returns an error" in new Test {
        MockDeleteLossClaimRequestParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockDeleteLossClaimService
          .delete(request)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, ClaimIdFormatError, None))))

        runErrorTestWithAudit(ClaimIdFormatError)
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking {

    private val controller = new DeleteLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockDeleteLossClaimService,
      parser = mockDeleteLossClaimRequestParser,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.delete(nino, claimId)(fakeRequest)

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetailOld] =
      AuditEvent(
        auditType = "DeleteLossClaim",
        transactionName = "delete-loss-claim",
        detail = GenericAuditDetailOld(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = "3.0",
          params = Map("nino" -> nino, "claimId" -> claimId),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

  }

}
