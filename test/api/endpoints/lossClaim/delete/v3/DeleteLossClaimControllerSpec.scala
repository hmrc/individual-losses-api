/*
 * Copyright 2022 HM Revenue & Customs
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

package api.endpoints.lossClaim.delete.v3

import api.controllers.ControllerBaseSpec
import api.endpoints.lossClaim.delete.v3.request.{DeleteLossClaimRawData, DeleteLossClaimRequest, MockDeleteLossClaimRequestDataParser}
import api.models.audit.{AuditError, AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.Nino
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import api.endpoints.lossClaim.delete.v3
import api.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteLossClaimControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockDeleteLossClaimService
    with MockDeleteLossClaimRequestDataParser
    with MockAuditService {

  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino: String          = "AA123456A"
  val claimId: String       = "AAZZ1234567890a"

  val rawData: DeleteLossClaimRawData = DeleteLossClaimRawData(nino, claimId)
  val request: DeleteLossClaimRequest = v3.request.DeleteLossClaimRequest(Nino(nino), claimId)

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "DeleteLossClaim",
      transactionName = "delete-loss-claim",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        versionNumber = "3.0",
        params = Map("nino" -> nino, "claimId" -> claimId),
        request = None,
        `X-CorrelationId` = correlationId,
        response = auditResponse
      )
    )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new DeleteLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      deleteLossClaimService = mockDeleteLossClaimService,
      deleteLossClaimParser = mockDeleteLossClaimRequestDataParser,
      auditService = mockAuditService,
      cc = cc
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
  }

  "delete" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockDeleteLossClaimRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockDeleteLossClaimService
          .delete(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        val result: Future[Result] = controller.delete(nino, claimId)(fakeRequest)
        status(result) shouldBe NO_CONTENT
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(NO_CONTENT, None, None)
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }

    "handle mdtp validation errors as per spec" when {
      def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the parser" in new Test {

          MockDeleteLossClaimRequestDataParser
            .parseRequest(rawData)
            .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

          val response: Future[Result] = controller.delete(nino, claimId)(fakeRequest)

          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
          header("X-CorrelationId", response) shouldBe Some(correlationId)

          val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None)
          MockedAuditService.verifyAuditEvent(event(auditResponse)).once
        }
      }

      errorsFromParserTester(BadRequestError, BAD_REQUEST)
      errorsFromParserTester(NinoFormatError, BAD_REQUEST)
      errorsFromParserTester(ClaimIdFormatError, BAD_REQUEST)

    }

    "handle downstream validation errors as per spec" when {
      def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the service" in new Test {

          MockDeleteLossClaimRequestDataParser
            .parseRequest(rawData)
            .returns(Right(request))

          MockDeleteLossClaimService
            .delete(request)
            .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

          val response: Future[Result] = controller.delete(nino, claimId)(fakeRequest)
          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
          header("X-CorrelationId", response) shouldBe Some(correlationId)

          val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None)
          MockedAuditService.verifyAuditEvent(event(auditResponse)).once
        }
      }

      errorsFromServiceTester(BadRequestError, BAD_REQUEST)
      errorsFromServiceTester(StandardDownstreamError, INTERNAL_SERVER_ERROR)
      errorsFromServiceTester(NotFoundError, NOT_FOUND)
      errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
      errorsFromServiceTester(ClaimIdFormatError, BAD_REQUEST)
    }
  }
}
