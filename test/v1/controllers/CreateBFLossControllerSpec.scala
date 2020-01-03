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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockCreateBFLossRequestDataParser
import v1.mocks.services.{MockAuditService, MockCreateBFLossService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, CreateBFLossAuditDetail}
import v1.models.des.{CreateBFLossHateoasData, CreateBFLossResponse}
import v1.models.domain.{BFLoss, TypeOfLoss}
import v1.models.errors.{NotFoundError, _}
import v1.models.hateoas.Method.GET
import v1.models.hateoas.{HateoasWrapper, Link}
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
    with MockHateoasFactory
    with MockAuditService {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino = "AA123456A"
  val lossId = "AAZZ1234567890a"

  val bfLoss = BFLoss(TypeOfLoss.`self-employment`, Some("XKIS00000000988"), "2019-20", 256.78)

  val createBFLossResponse = CreateBFLossResponse("AAZZ1234567890a")
  val testHateoasLink = Link(href = "/foo/bar", method = GET, rel="test-relationship")

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
      |  "id": "AAZZ1234567890a",
      |  "links" : [
      |     {
      |       "href": "/foo/bar",
      |       "method": "GET",
      |       "rel": "test-relationship"
      |     }
      |  ]
      |}
    """.stripMargin)

  trait Test {
    val hc = HeaderCarrier()

    val controller = new CreateBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      createBFLossService = mockCreateBFLossService,
      createBFLossParser = mockCreateBFLossRequestDataParser,
      hateoasFactory = mockHateoasFactory,
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

        MockHateoasFactory
          .wrap(createBFLossResponse, CreateBFLossHateoasData(nino, lossId))
          .returns(HateoasWrapper(createBFLossResponse, Seq(testHateoasLink)))

        val result: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        status(result) shouldBe CREATED
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val detail = CreateBFLossAuditDetail(
          "Individual", None, nino,  requestBody, correlationId,
          AuditResponse(CREATED, None, Some(responseBody)))
        val event = AuditEvent("createBroughtForwardLoss", "create-brought-forward-loss", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }
  }

  "handle mdtp validation errors as per spec" when {
    def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockCreateBFLossRequestDataParser.
          parseRequest(CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val response: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail = CreateBFLossAuditDetail(
          "Individual", None, nino,  requestBody, correlationId,
          AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
        val event = AuditEvent("createBroughtForwardLoss", "create-brought-forward-loss", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

      val badRequestErrorsFromParser = List(
        NinoFormatError,
        TaxYearFormatError,
        RuleIncorrectOrEmptyBodyError,
        RuleTaxYearNotSupportedError,
        RuleTaxYearRangeInvalid,
        TypeOfLossFormatError,
        SelfEmploymentIdFormatError,
        RuleSelfEmploymentId,
        AmountFormatError,
        RuleInvalidLossAmount,
        RuleTaxYearNotEndedError
      )

    badRequestErrorsFromParser.foreach(errorsFromParserTester(_, BAD_REQUEST))
  }

  "handle non-mdtp validation errors as per spec" when {
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

        val detail = CreateBFLossAuditDetail(
          "Individual", None, nino,  requestBody, correlationId,
          AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
        val event = AuditEvent("createBroughtForwardLoss", "create-brought-forward-loss", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    errorsFromServiceTester(BadRequestError, BAD_REQUEST)
    errorsFromServiceTester(DownstreamError, INTERNAL_SERVER_ERROR)
    errorsFromServiceTester(RuleDuplicateSubmissionError, FORBIDDEN)
    errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
    errorsFromServiceTester(NotFoundError, NOT_FOUND)
  }
}
