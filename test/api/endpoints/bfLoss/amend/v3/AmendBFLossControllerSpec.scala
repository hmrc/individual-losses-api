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

package api.endpoints.bfLoss.amend.v3

import api.controllers.ControllerBaseSpec
import api.endpoints.bfLoss.amend.anyVersion.request.{ AmendBFLossRawData, AmendBFLossRequestBody }
import api.endpoints.bfLoss.amend.anyVersion.response.AmendBFLossHateoasData
import api.endpoints.bfLoss.amend.v3.request.{ AmendBFLossRequest, MockAmendBFLossParser }
import api.endpoints.bfLoss.amend.v3.response.AmendBFLossResponse
import api.endpoints.bfLoss.domain.v3.TypeOfLoss
import api.hateoas.MockHateoasFactory
import api.mocks.MockIdGenerator
import api.models.ResponseWrapper
import api.models.audit.{ AuditError, AuditEvent, AuditResponse, GenericAuditDetail }
import api.models.domain.Nino
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{ HateoasWrapper, Link }
import api.services.{ MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, Result }
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendBFLossControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAmendBFLossService
    with MockAmendBFLossParser
    with MockHateoasFactory
    with MockAuditService
    with MockIdGenerator {

  val nino: String           = "AA123456A"
  val lossId: String         = "AAZZ1234567890a"
  val lossAmount: BigDecimal = BigDecimal(2345.67)

  val amendBFLoss: AmendBFLossRequestBody = AmendBFLossRequestBody(lossAmount)

  val amendBFLossResponse: AmendBFLossResponse = AmendBFLossResponse(
    businessId = "XBIS12345678910",
    typeOfLoss = TypeOfLoss.`self-employment`,
    lossAmount = lossAmount,
    taxYearBroughtForwardFrom = "2021-22",
    lastModified = "2022-07-13T12:13:48.763Z"
  )

  val testHateoasLink: Link = Link(href = "/individuals/losses/TC663795B/brought-forward-losses/AAZZ1234567890a", method = GET, rel = "self")

  val bfLossRequest: AmendBFLossRequest = AmendBFLossRequest(Nino(nino), lossId, amendBFLoss)

  val responseBody: JsValue = Json.parse(
    s"""
      |{
      |    "businessId": "XBIS12345678910",
      |    "typeOfLoss": "self-employment",
      |    "lossAmount": $lossAmount,
      |    "taxYearBroughtForwardFrom": "2021-22",
      |    "lastModified": "2022-07-13T12:13:48.763Z",
      |    "links" : [
      |     {
      |       "href": "/individuals/losses/TC663795B/brought-forward-losses/AAZZ1234567890a",
      |        "rel": "self",
      |        "method": "GET"
      |     }
      |  ]
      |}
    """.stripMargin
  )

  val requestBody: JsValue = Json.parse(
    s"""
      |{
      |  "lossAmount": $lossAmount
      |}
    """.stripMargin
  )

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "AmendBroughtForwardLoss",
      transactionName = "amend-brought-forward-loss",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        versionNumber = "3.0",
        params = Map("nino" -> nino, "lossId" -> lossId),
        request = Some(requestBody),
        `X-CorrelationId` = correlationId,
        response = auditResponse
      )
    )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new AmendBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      amendBFLossService = mockAmendBFLossService,
      amendBFLossParser = mockAmendBFLossRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
    MockIdGenerator.getCorrelationId.returns(correlationId)
  }

  "amend" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockAmendBFLossRequestDataParser
          .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
          .returns(Right(bfLossRequest))

        MockAmendBFLossService
          .amend(AmendBFLossRequest(Nino(nino), lossId, amendBFLoss))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, amendBFLossResponse))))

        MockHateoasFactory
          .wrap(amendBFLossResponse, AmendBFLossHateoasData(nino, lossId))
          .returns(HateoasWrapper(amendBFLossResponse, Seq(testHateoasLink)))

        val result: Future[Result] = controller.amend(nino, lossId)(fakePostRequest(requestBody))
        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(OK, None, Some(responseBody))
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }

    "return the error as per spec" when {
      "parser errors occur" should {
        def errorsFromParserTester(error: MtdError): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockAmendBFLossRequestDataParser
              .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
              .returns(Left(ErrorWrapper(correlationId, error, None)))

            val result: Future[Result] = controller.amend(nino, lossId)(fakePostRequest(requestBody))

            status(result) shouldBe error.httpStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(error.httpStatus, Some(Seq(AuditError(error.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once
          }
        }

        val input = Seq(
          BadRequestError,
          NinoFormatError,
          LossIdFormatError,
          ValueFormatError.copy(paths = Some(Seq("/lossAmount")))
        )

        input.foreach(errorsFromParserTester)
      }

      "service errors occur" should {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockAmendBFLossRequestDataParser
              .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
              .returns(Right(bfLossRequest))

            MockAmendBFLossService
              .amend(AmendBFLossRequest(Nino(nino), lossId, amendBFLoss))
              .returns(Future.successful(Left(ErrorWrapper(correlationId, mtdError))))

            val result: Future[Result] = controller.amend(nino, lossId)(fakePostRequest(requestBody))

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(mtdError.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once
          }
        }

        val input = Seq(
          (BadRequestError, BAD_REQUEST),
          (NinoFormatError, BAD_REQUEST),
          (LossIdFormatError, BAD_REQUEST),
          (RuleLossAmountNotChanged, FORBIDDEN),
          (NotFoundError, NOT_FOUND),
          (StandardDownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}
