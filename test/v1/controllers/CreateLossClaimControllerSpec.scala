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
import v1.mocks.requestParsers.MockCreateLossClaimRequestDataParser
import v1.mocks.services._
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, CreateLossClaimAuditDetail}
import v1.models.des.{CreateLossClaimHateoasData, CreateLossClaimResponse}
import v1.models.domain.{LossClaim, TypeOfClaim, TypeOfLoss}
import v1.models.errors.{NotFoundError, _}
import v1.models.hateoas.Method.GET
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.outcomes.DesResponse
import v1.models.requestData.{CreateLossClaimRawData, CreateLossClaimRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateLossClaimControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockCreateLossClaimService
    with MockCreateLossClaimRequestDataParser
    with MockHateoasFactory
    with MockAuditService {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino = "AA123456A"
  val lossClaimId = "AAZZ1234567890a"

  val lossClaim = LossClaim("2017-18", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-sideways`, Some("XKIS00000000988"))

  val createLossClaimResponse = CreateLossClaimResponse("AAZZ1234567890a")
  val testHateoasLink = Link(href = "/foo/bar", method = GET, rel="test-relationship")

  val lossClaimRequest: CreateLossClaimRequest = CreateLossClaimRequest(Nino(nino), lossClaim)

  val requestBody: JsValue = Json.parse("""
      |{
      |    "selfEmploymentId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYear": "2017-18",
      |    "typeOfClaim": "carry-forward"
      |}
    """.stripMargin)

  val responseBody: JsValue = Json.parse("""
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

    val controller = new CreateLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      createLossClaimService = mockCreateLossClaimService,
      createLossClaimParser = mockCreateLossClaimRequestDataParser,
      auditService = mockAuditService,
      hateoasFactory = mockHateoasFactory,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "create" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockCreateLossClaimRequestDataParser
          .parseRequest(CreateLossClaimRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Right(lossClaimRequest))

        MockCreateLossClaimService
          .create(CreateLossClaimRequest(Nino(nino), lossClaim))
          .returns(Future.successful(Right(DesResponse(correlationId, createLossClaimResponse))))

        MockHateoasFactory
          .wrap(createLossClaimResponse, CreateLossClaimHateoasData(nino, lossClaimId))
          .returns(HateoasWrapper(createLossClaimResponse, Seq(testHateoasLink)))

        val result: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        status(result) shouldBe CREATED
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val detail = CreateLossClaimAuditDetail(
          "Individual", None, nino,  requestBody, correlationId,
          AuditResponse(CREATED, None, Some(responseBody)))
        val event = AuditEvent("createLossClaim", "create-loss-claim", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }
  }

  "handle mdtp validation errors as per spec" when {
    def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockCreateLossClaimRequestDataParser
          .parseRequest(CreateLossClaimRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val response: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail = CreateLossClaimAuditDetail(
          "Individual", None, nino,  requestBody, correlationId,
          AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
        val event = AuditEvent("createLossClaim", "create-loss-claim", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    val badRequestErrorsFromParser = List(
      BadRequestError,
      NinoFormatError,
      TaxYearFormatError,
      RuleIncorrectOrEmptyBodyError,
      RuleTaxYearNotSupportedError,
      RuleTaxYearRangeInvalid,
      TypeOfLossFormatError,
      SelfEmploymentIdFormatError,
      RuleSelfEmploymentId,
      RuleTypeOfClaimInvalid,
      TypeOfClaimFormatError
    )

    badRequestErrorsFromParser.foreach(errorsFromParserTester(_, BAD_REQUEST))
  }

  "handle non-mdtp validation errors as per spec" when {
    def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockCreateLossClaimRequestDataParser
          .parseRequest(CreateLossClaimRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Right(lossClaimRequest))

        MockCreateLossClaimService
          .create(CreateLossClaimRequest(Nino(nino), lossClaim))
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val response: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail = CreateLossClaimAuditDetail(
          "Individual", None, nino,  requestBody, correlationId,
          AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
        val event = AuditEvent("createLossClaim", "create-loss-claim", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    errorsFromServiceTester(BadRequestError, BAD_REQUEST)
    errorsFromServiceTester(DownstreamError, INTERNAL_SERVER_ERROR)
    errorsFromServiceTester(RuleDuplicateClaimSubmissionError, FORBIDDEN)
    errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
    errorsFromServiceTester(NotFoundError, NOT_FOUND)
    errorsFromServiceTester(RuleTypeOfClaimInvalid, BAD_REQUEST)
    errorsFromServiceTester(RulePeriodNotEnded, FORBIDDEN)
    errorsFromServiceTester(RuleNoAccountingPeriod, FORBIDDEN)
  }
}
