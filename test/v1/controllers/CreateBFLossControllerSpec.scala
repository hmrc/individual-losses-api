/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.requestParsers.MockCreateBFLossRequestDataParser
import v1.mocks.services.{MockAuditService, MockCreateBFLossService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.des.CreateBFLossResponse
import v1.models.domain.BFLoss
import v1.models.errors.{NotFoundError, _}
import v1.models.outcomes.DesResponse
import v1.models.requestData.{CreateBFLossRawData, CreateBFLossRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateBFLossControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockCreateBFLossService
    with MockCreateBFLossRequestDataParser
    with MockAuditService {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino = "AA123456A"
  val lossId = "AAZZ1234567890a"

  val bfLoss = BFLoss("self-employment", Some("XKIS00000000988"), "2019-20", 256.78)

  val createBFLossResponse = CreateBFLossResponse("AAZZ1234567890a")

  val bfLossRequest: CreateBFLossRequest = CreateBFLossRequest(Nino(nino), bfLoss)

  val requestBody: JsValue = Json.parse(
    """
      |{
      |    "selfEmploymentId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYear": "2019-20",
      |    "lossAmount": 256.78
      |}
    """.stripMargin)

  val responseBody: JsValue = Json.parse(
    """
      |{
      |  "id": "AAZZ1234567890a"
      |}
    """.stripMargin)

  trait Test {
    val hc = HeaderCarrier()

    val controller = new CreateBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      createBFLossService = mockCreateBFLossService,
      createBFLossParser = mockCreateBFLossRequestDataParser,
      auditService = mockAuditService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "create" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockCreateBFLossRequestDataParser.parseRequest(
          CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Right(bfLossRequest))

        MockCreateBFLossService
          .create(CreateBFLossRequest(Nino(nino), bfLoss))
          .returns(Future.successful(Right(DesResponse(correlationId, createBFLossResponse))))

        val result: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        status(result) shouldBe CREATED
        contentAsJson(result) shouldBe responseBody
      }
    }

    "return single error response with status 400" when {
      "the request received failed the validation" in new Test() {

        MockCreateBFLossRequestDataParser.parseRequest(
          CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(None, NinoFormatError, None)))

        val result: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        status(result) shouldBe BAD_REQUEST
        header("X-CorrelationId", result).nonEmpty shouldBe true
      }
    }
    "return a 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        NinoFormatError,
        TaxYearFormatError,
        RuleIncorrectOrEmptyBodyError,
        RuleTaxYearNotSupportedError,
        RuleTaxYearRangeExceededError,
        TypeOfLossUnsupportedFormatError,
        SelfEmploymentIdFormatError,
        RulePropertySelfEmploymentId,
        AmountFormatError,
        RuleInvalidLossAmount
      )

      val notFoundErrorsFromService = List(
        NotFoundError
      )

      val forbiddenErrorsFromService = List(
        RuleDuplicateSubmissionError
      )

      forbiddenErrorsFromService.foreach(errorsFromServiceTester(_, FORBIDDEN))
      badRequestErrorsFromParser.foreach(errorsFromParserTester(_, BAD_REQUEST))
      notFoundErrorsFromService.foreach(errorsFromServiceTester(_, NOT_FOUND))
    }
  }

  def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
    s"a ${error.code} error is returned from the parser" in new Test {

      MockCreateBFLossRequestDataParser.
        parseRequest(CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
        .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

      val response: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))

      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
      header("X-CorrelationId", response) shouldBe Some(correlationId)

    }
  }

  def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
    s"a ${error.code} error is returned from the service" in new Test {

      MockCreateBFLossRequestDataParser.parseRequest(
        CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
        .returns(Right(bfLossRequest))

      MockCreateBFLossService
        .create(CreateBFLossRequest(Nino(nino), bfLoss))
        .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

      val response: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
      header("X-CorrelationId", response) shouldBe Some(correlationId)

    }
  }
}
