/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.controllers

import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.requestParsers.MockDeleteBFLossRequestDataParser
import v1.mocks.services.{MockAuditService, MockDeleteBFLossService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, DeleteBFLossAuditDetail}
import v1.models.errors.{NotFoundError, _}
import v1.models.outcomes.DesResponse
import v1.models.requestData.{DeleteBFLossRawData, DeleteBFLossRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteBFLossControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockDeleteBFLossService
    with MockDeleteBFLossRequestDataParser
    with MockAuditService {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino   = "AA123456A"
  val lossId = "AAZZ1234567890a"

  val rawData = DeleteBFLossRawData(nino, lossId)
  val request = DeleteBFLossRequest(Nino(nino), lossId)

  trait Test {
    val hc = HeaderCarrier()

    val controller = new DeleteBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      deleteBFLossService = mockDeleteBFLossService,
      deleteBFLossParser = mockDeleteBFLossRequestDataParser,
      auditService = mockAuditService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "delete" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockDeleteBFLossRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockDeleteBFLossService
          .delete(request)
          .returns(Future.successful(Right(DesResponse(correlationId, ()))))

        val result: Future[Result] = controller.delete(nino, lossId)(fakeRequest)
        status(result) shouldBe NO_CONTENT
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val detail = DeleteBFLossAuditDetail(
          "Individual", None, nino,  lossId, correlationId, AuditResponse(NO_CONTENT, None, None))
        val event = AuditEvent("deleteBroughtForwardLoss", "delete-brought-forward-Loss", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "handle mdtp validation errors as per spec" when {
      def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the parser" in new Test {

          MockDeleteBFLossRequestDataParser
            .parseRequest(rawData)
            .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

          val response: Future[Result] = controller.delete(nino, lossId)(fakeRequest)

          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
          header("X-CorrelationId", response) shouldBe Some(correlationId)

          val detail = DeleteBFLossAuditDetail(
            "Individual", None, nino, lossId, correlationId,
            AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
          val event = AuditEvent("deleteBroughtForwardLoss", "delete-brought-forward-Loss", detail)
          MockedAuditService.verifyAuditEvent(event).once
        }
      }

      errorsFromParserTester(BadRequestError, BAD_REQUEST)
      errorsFromParserTester(NotFoundError, NOT_FOUND)
      errorsFromParserTester(NinoFormatError, BAD_REQUEST)
      errorsFromParserTester(LossIdFormatError, BAD_REQUEST)

    }

    "handle non-mdtp validation errors as per spec" when {
      def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the service" in new Test {

          MockDeleteBFLossRequestDataParser
            .parseRequest(rawData)
            .returns(Right(request))

          MockDeleteBFLossService
            .delete(request)
            .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

          val response: Future[Result] = controller.delete(nino, lossId)(fakeRequest)
          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
          header("X-CorrelationId", response) shouldBe Some(correlationId)

          val detail = DeleteBFLossAuditDetail(
            "Individual", None, nino, lossId, correlationId,
            AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
          val event = AuditEvent("deleteBroughtForwardLoss", "delete-brought-forward-Loss", detail)
          MockedAuditService.verifyAuditEvent(event).once
        }
      }

      errorsFromServiceTester(BadRequestError, BAD_REQUEST)
      errorsFromServiceTester(DownstreamError, INTERNAL_SERVER_ERROR)
      errorsFromServiceTester(NotFoundError, NOT_FOUND)
      errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
      errorsFromServiceTester(LossIdFormatError, BAD_REQUEST)
      errorsFromServiceTester(RuleDeleteAfterCrystallisationError, FORBIDDEN)
    }
  }
}
