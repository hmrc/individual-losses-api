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

package api.endpoints.createBFLoss.v2

import api.controllers.ControllerBaseSpec
import api.endpoints.amendBFLoss.v2.domain
import api.endpoints.amendBFLoss.v2.domain.BFLoss
import api.endpoints.createBFLoss.v2.model.audit.CreateBFLossAuditDetail
import api.endpoints.createBFLoss.v2.model.downstream.{ CreateBFLossHateoasData, CreateBFLossResponse }
import api.endpoints.createBFLoss.v2.model.request
import api.endpoints.createBFLoss.v2.model.request.{ CreateBFLossRawData, CreateBFLossRequest }
import api.mocks.hateoas.MockHateoasFactory
import api.mocks.services.{ MockEnrolmentsAuthService, MockMtdIdLookupService }
import api.models.audit.{ AuditError, AuditEvent, AuditResponse }
import api.models.domain.Nino
import api.models.domain.lossClaim.v2.TypeOfLoss
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{ HateoasWrapper, Link }
import api.models.outcomes.ResponseWrapper
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, Result }
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.requestParsers.MockCreateBFLossRequestDataParser
import v2.mocks.services.{ MockAuditService, MockCreateBFLossService }
import v2.models.errors._

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

  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino: String          = "AA123456A"
  val lossId: String        = "AAZZ1234567890a"

  val bfLoss: BFLoss = domain.BFLoss(TypeOfLoss.`self-employment`, Some("XKIS00000000988"), "2019-20", 256.78)

  val createBFLossResponse: CreateBFLossResponse = CreateBFLossResponse("AAZZ1234567890a")

  val testHateoasLink: Link = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  val bfLossRequest: CreateBFLossRequest = request.CreateBFLossRequest(Nino(nino), bfLoss)

  val requestBody: JsValue = Json.parse(
    """
      |{
      |    "businessId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYear": "2019-20",
      |    "lossAmount": 256.78
      |}
    """.stripMargin
  )

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
    """.stripMargin
  )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new CreateBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      createBFLossService = mockCreateBFLossService,
      createBFLossParser = mockCreateBFLossRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
  }

  "create" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockCreateBFLossRequestDataParser
          .parseRequest(CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Right(bfLossRequest))

        MockCreateBFLossService
          .create(request.CreateBFLossRequest(Nino(nino), bfLoss))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, createBFLossResponse))))

        MockHateoasFactory
          .wrap(createBFLossResponse, CreateBFLossHateoasData(nino, lossId))
          .returns(HateoasWrapper(createBFLossResponse, Seq(testHateoasLink)))

        val result: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        status(result) shouldBe CREATED
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val detail: CreateBFLossAuditDetail =
          CreateBFLossAuditDetail("Individual", None, nino, requestBody, correlationId, AuditResponse(CREATED, None, Some(responseBody)))
        val event: AuditEvent[CreateBFLossAuditDetail] = AuditEvent("createBroughtForwardLoss", "create-brought-forward-loss", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }
  }

  "handle mdtp validation errors as per spec" when {
    def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockCreateBFLossRequestDataParser
          .parseRequest(request.CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val response: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail: CreateBFLossAuditDetail = CreateBFLossAuditDetail("Individual",
                                                                      None,
                                                                      nino,
                                                                      requestBody,
                                                                      correlationId,
                                                                      AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
        val event: AuditEvent[CreateBFLossAuditDetail] = AuditEvent("createBroughtForwardLoss", "create-brought-forward-loss", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    val badRequestErrorsFromParser = List(
      NinoFormatError,
      TaxYearFormatError.copy(paths = Some(List("/taxYear"))),
      RuleIncorrectOrEmptyBodyError,
      RuleTaxYearNotSupportedError,
      RuleTaxYearRangeInvalid.copy(paths = Some(List("/taxYear"))),
      TypeOfLossFormatError,
      BusinessIdFormatError,
      RuleBusinessId,
      AmountFormatError,
      RuleInvalidLossAmount,
      RuleTaxYearNotEndedError
    )

    badRequestErrorsFromParser.foreach(errorsFromParserTester(_, BAD_REQUEST))
  }

  "handle non-mdtp validation errors as per spec" when {
    def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockCreateBFLossRequestDataParser
          .parseRequest(request.CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Right(bfLossRequest))

        MockCreateBFLossService
          .create(request.CreateBFLossRequest(Nino(nino), bfLoss))
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val response: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail: CreateBFLossAuditDetail = CreateBFLossAuditDetail("Individual",
                                                                      None,
                                                                      nino,
                                                                      requestBody,
                                                                      correlationId,
                                                                      AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
        val event: AuditEvent[CreateBFLossAuditDetail] = AuditEvent("createBroughtForwardLoss", "create-brought-forward-loss", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    errorsFromServiceTester(BadRequestError, BAD_REQUEST)
    errorsFromServiceTester(RuleBusinessId, BAD_REQUEST)
    errorsFromServiceTester(StandardDownstreamError, INTERNAL_SERVER_ERROR)
    errorsFromServiceTester(RuleDuplicateSubmissionError, FORBIDDEN)
    errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
    errorsFromServiceTester(NotFoundError, NOT_FOUND)
  }
}
