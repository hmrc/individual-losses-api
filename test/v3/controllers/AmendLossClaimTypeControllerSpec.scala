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

package v3.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.http.HeaderCarrier
import v3.mocks.hateoas.MockHateoasFactory
import v3.mocks.requestParsers.MockAmendLossClaimTypeRequestDataParser
import v3.mocks.services._
import v3.models.audit.{AmendLossClaimTypeAuditDetail, AuditError, AuditEvent, AuditResponse}
import v3.models.downstream.{AmendLossClaimTypeHateoasData, LossClaimResponse}
import v3.models.domain.{AmendLossClaimTypeRequestBody, Nino, TypeOfClaim, TypeOfLoss}
import v3.models.errors.{NinoFormatError, NotFoundError, RuleIncorrectOrEmptyBodyError, _}
import v3.models.hateoas.Method.GET
import v3.models.hateoas.{HateoasWrapper, Link}
import v3.models.outcomes.ResponseWrapper
import v3.models.requestData.{AmendLossClaimTypeRawData, AmendLossClaimTypeRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendLossClaimTypeControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAmendLossClaimTypeService
    with MockAmendLossClaimTypeRequestDataParser
    with MockHateoasFactory
    with MockAuditService {

  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino: String = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  val amendLossClaimType: AmendLossClaimTypeRequestBody = AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

  val response: LossClaimResponse =
    LossClaimResponse(
      "2019-20",
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      "XKIS00000000988",
      Some(1),
      "2018-07-13T12:13:48.763Z"
    )

  val request: AmendLossClaimTypeRequest = AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType)

  val testHateoasLink: Link = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  val responseBody: JsValue = Json.parse(
    """
      |{
      |    "businessId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYearClaimedFor": "2019-20",
      |    "typeOfClaim": "carry-forward",
      |    "sequence": 1,
      |    "lastModified": "2018-07-13T12:13:48.763Z",
      |    "links" : [
      |      {
      |        "href": "/foo/bar",
      |        "method": "GET",
      |        "rel": "test-relationship"
      |      }
      |    ]
      |}
   """.stripMargin
  )

  val requestBody: JsValue = Json.parse(
    """
      |{
      |  "typeOfClaim": "carry-forward"
      |}
   """.stripMargin
  )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new AmendLossClaimTypeController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      amendLossClaimTypeService = mockAmendLossClaimTypeService,
      amendLossClaimTypeParser = mockAmendLossClaimTypeRequestDataParser,
      auditService = mockAuditService,
      hateoasFactory = mockHateoasFactory,
      cc = cc
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
  }

  "amend" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockAmendLossClaimTypeRequestDataParser.parseRequest(
          AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Right(request))

        MockAmendLossClaimTypeService
          .amend(AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrap(response, AmendLossClaimTypeHateoasData(nino, claimId))
          .returns(HateoasWrapper(response, Seq(testHateoasLink)))

        val result: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))
        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val detail: AmendLossClaimTypeAuditDetail = AmendLossClaimTypeAuditDetail(
          "Individual", None, nino,  claimId, requestBody, correlationId,
          AuditResponse(OK, None, Some(responseBody)))
        val event: AuditEvent[AmendLossClaimTypeAuditDetail] = AuditEvent("amendLossClaimType", "amend-loss-claim-type", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "return a 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        NinoFormatError,
        RuleIncorrectOrEmptyBodyError,
        ClaimIdFormatError,
        TypeOfClaimFormatError
      )

      val badRequestErrorsFromService = List(
        NinoFormatError,
        ClaimIdFormatError
      )

      val notFoundErrorsFromService = List(
        NotFoundError
      )

      val forbiddenErrorsFromService = List(
        RuleClaimTypeNotChanged,
        RuleTypeOfClaimInvalid
      )

      badRequestErrorsFromParser.foreach(errorsFromParserTester(_, BAD_REQUEST))
      badRequestErrorsFromService.foreach(errorsFromServiceTester(_, BAD_REQUEST))
      notFoundErrorsFromService.foreach(errorsFromServiceTester(_, NOT_FOUND))
      forbiddenErrorsFromService.foreach(errorsFromServiceTester(_, FORBIDDEN))
      errorsFromServiceTester(DownstreamError, INTERNAL_SERVER_ERROR)
    }
  }

  def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
    s"a ${error.code} error is returned from the parser" in new Test {

      MockAmendLossClaimTypeRequestDataParser.
        parseRequest(AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
        .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

      val response: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))

      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
      header("X-CorrelationId", response) shouldBe Some(correlationId)

      val detail: AmendLossClaimTypeAuditDetail = AmendLossClaimTypeAuditDetail(
        "Individual", None, nino, claimId, requestBody, correlationId,
        AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
      val event: AuditEvent[AmendLossClaimTypeAuditDetail] = AuditEvent("amendLossClaimType", "amend-loss-claim-type", detail)
      MockedAuditService.verifyAuditEvent(event).once
    }
  }

  def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
    s"a ${error.code} error is returned from the service" in new Test {

      MockAmendLossClaimTypeRequestDataParser.parseRequest(
        AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
        .returns(Right(request))

      MockAmendLossClaimTypeService
        .amend(AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType))
        .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

      val response: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))
      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
      header("X-CorrelationId", response) shouldBe Some(correlationId)

      val detail: AmendLossClaimTypeAuditDetail = AmendLossClaimTypeAuditDetail(
        "Individual", None, nino, claimId, requestBody, correlationId,
        AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
      val event: AuditEvent[AmendLossClaimTypeAuditDetail] = AuditEvent("amendLossClaimType", "amend-loss-claim-type", detail)
      MockedAuditService.verifyAuditEvent(event).once
    }
  }
}