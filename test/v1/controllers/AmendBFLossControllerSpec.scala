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
import v1.mocks.requestParsers.MockAmendBFLossRequestDataParser
import v1.mocks.services._
import v1.models.audit.{AmendBFLossAuditDetail, AuditError, AuditEvent, AuditResponse}
import v1.models.des.{AmendBFLossHateoasData, BFLossResponse}
import v1.models.domain.{AmendBFLoss, TypeOfLoss}
import v1.models.errors._
import v1.models.hateoas.Method.GET
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.outcomes.DesResponse
import v1.models.requestData.{AmendBFLossRawData, AmendBFLossRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendBFLossControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAmendBFLossService
    with MockAmendBFLossRequestDataParser
    with MockHateoasFactory
    with MockAuditService {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino        = "AA123456A"
  val lossId      = "AAZZ1234567890a"
  val lossAmount  = BigDecimal(531.99)
  val amendBFLoss = AmendBFLoss(lossAmount)

  val amendBFLossResponse = BFLossResponse(
    selfEmploymentId = Some("XKIS00000000988"),
    typeOfLoss = TypeOfLoss.`self-employment`,
    lossAmount = lossAmount,
    taxYear = "2019-20",
    lastModified = "2018-07-13T12:13:48.763Z"
  )

  val testHateoasLink = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  val bfLossRequest: AmendBFLossRequest = AmendBFLossRequest(Nino(nino), lossId, amendBFLoss)

  val responseBody: JsValue = Json.parse(s"""
      |{
      |    "selfEmploymentId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYear": "2019-20",
      |    "lossAmount": $lossAmount,
      |    "lastModified": "2018-07-13T12:13:48.763Z",
      |    "links" : [
      |     {
      |       "href": "/foo/bar",
      |       "method": "GET",
      |       "rel": "test-relationship"
      |     }
      |  ]
      |}
    """.stripMargin)

  val requestBody: JsValue = Json.parse(s"""
      |{
      |  "lossAmount": $lossAmount
      |}
    """.stripMargin)

  trait Test {
    val hc = HeaderCarrier()

    val controller = new AmendBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      amendBFLossService = mockAmendBFLossService,
      hateoasFactory = mockHateoasFactory,
      amendBFLossParser = mockAmendBFLossRequestDataParser,
      auditService = mockAuditService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "amend" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockAmendBFLossRequestDataParser
          .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
          .returns(Right(bfLossRequest))

        MockAmendBFLossService
          .amend(AmendBFLossRequest(Nino(nino), lossId, amendBFLoss))
          .returns(Future.successful(Right(DesResponse(correlationId, amendBFLossResponse))))

        MockHateoasFactory
          .wrap(amendBFLossResponse, AmendBFLossHateoasData(nino, lossId))
          .returns(HateoasWrapper(amendBFLossResponse, Seq(testHateoasLink)))

        val result: Future[Result] = controller.amend(nino, lossId)(fakePostRequest(requestBody))
        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val detail = AmendBFLossAuditDetail(
          "Individual", None, nino,  lossId, requestBody, correlationId,
          AuditResponse(OK, None, Some(responseBody)))
        val event = AuditEvent("amendBroughtForwardLoss", "amend-brought-forward-Loss", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "return single error response with status 400" when {
      "the request received failed the validation" in new Test() {

        MockAmendBFLossRequestDataParser
          .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(None, NinoFormatError, None)))

        val result: Future[Result] = controller.amend(nino, lossId)(fakePostRequest(requestBody))
        status(result) shouldBe BAD_REQUEST
        header("X-CorrelationId", result).nonEmpty shouldBe true
      }
    }
    "return a 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        NinoFormatError,
        RuleIncorrectOrEmptyBodyError,
        LossIdFormatError,
        AmountFormatError,
        RuleInvalidLossAmount
      )

      val badRequestErrorsFromService = List(
        NinoFormatError,
        LossIdFormatError
      )

      val notFoundErrorsFromService = List(
        NotFoundError
      )

      badRequestErrorsFromParser.foreach(errorsFromParserTester(_, BAD_REQUEST))
      badRequestErrorsFromService.foreach(errorsFromServiceTester(_, BAD_REQUEST))
      notFoundErrorsFromService.foreach(errorsFromServiceTester(_, NOT_FOUND))
      errorsFromServiceTester(RuleLossAmountNotChanged, FORBIDDEN)
      errorsFromServiceTester(DownstreamError, INTERNAL_SERVER_ERROR)
    }
  }

  def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
    s"a ${error.code} error is returned from the parser" in new Test {

      MockAmendBFLossRequestDataParser
        .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
        .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

      val response: Future[Result] = controller.amend(nino, lossId)(fakePostRequest(requestBody))

      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
      header("X-CorrelationId", response) shouldBe Some(correlationId)

      val detail = AmendBFLossAuditDetail(
        "Individual", None, nino, lossId, requestBody, correlationId,
        AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
      val event = AuditEvent("amendBroughtForwardLoss", "amend-brought-forward-Loss", detail)
      MockedAuditService.verifyAuditEvent(event).once

    }
  }

  def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
    s"a ${error.code} error is returned from the service" in new Test {

      MockAmendBFLossRequestDataParser
        .parseRequest(AmendBFLossRawData(nino, lossId, AnyContentAsJson(requestBody)))
        .returns(Right(bfLossRequest))

      MockAmendBFLossService
        .amend(AmendBFLossRequest(Nino(nino), lossId, amendBFLoss))
        .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

      val response: Future[Result] = controller.amend(nino, lossId)(fakePostRequest(requestBody))
      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
      header("X-CorrelationId", response) shouldBe Some(correlationId)

      val detail = AmendBFLossAuditDetail(
        "Individual", None, nino, lossId, requestBody, correlationId,
        AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
      val event = AuditEvent("amendBroughtForwardLoss", "amend-brought-forward-Loss", detail)
      MockedAuditService.verifyAuditEvent(event).once

    }
  }
}
