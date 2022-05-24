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

package api.endpoints.lossClaim.create.v3

import api.controllers.ControllerBaseSpec
import api.endpoints.lossClaim.create.v3.request.{CreateLossClaimRawData, CreateLossClaimRequest, CreateLossClaimRequestBody, MockCreateLossClaimParser}
import api.endpoints.lossClaim.create.v3.response.{CreateLossClaimHateoasData, CreateLossClaimResponse}
import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.hateoas.MockHateoasFactory
import api.models.audit.{AuditError, AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.Nino
import api.models.errors._
import api.models.errors.v3._
import api.models.hateoas.Method.GET
import api.models.hateoas.{HateoasWrapper, Link}
import api.models.outcomes.ResponseWrapper
import api.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateLossClaimControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockCreateLossClaimService
    with MockCreateLossClaimParser
    with MockHateoasFactory
    with MockAuditService {

  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino: String          = "AA123456A"
  val lossClaimId: String   = "AAZZ1234567890a"

  val lossClaim: CreateLossClaimRequestBody =
    CreateLossClaimRequestBody("2017-18", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-sideways`, "XKIS00000000988")

  val createLossClaimResponse: CreateLossClaimResponse = CreateLossClaimResponse("AAZZ1234567890a")

  val testHateoasLink: Link = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  val lossClaimRequest: CreateLossClaimRequest = request.CreateLossClaimRequest(Nino(nino), lossClaim)

  val requestBody: JsValue = Json.parse(
    """
      |{
      |    "selfEmploymentId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYear": "2017-18",
      |    "typeOfClaim": "carry-forward"
      |}
    """.stripMargin
  )

  val responseBody: JsValue = Json.parse(
    """
      |{
      |  "claimId": "AAZZ1234567890a",
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

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "CreateLossClaim",
      transactionName = "create-loss-claim",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        versionNumber = "3.0",
        params = Map("nino" -> nino),
        request = Some(requestBody),
        `X-CorrelationId` = correlationId,
        response = auditResponse
      )
    )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new CreateLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      createLossClaimService = mockCreateLossClaimService,
      createLossClaimParser = mockCreateLossClaimParser,
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

        MockCreateLossClaimRequestDataParser
          .parseRequest(CreateLossClaimRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Right(lossClaimRequest))

        MockCreateLossClaimService
          .create(request.CreateLossClaimRequest(Nino(nino), lossClaim))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, createLossClaimResponse))))

        MockHateoasFactory
          .wrap(createLossClaimResponse, CreateLossClaimHateoasData(nino, lossClaimId))
          .returns(HateoasWrapper(createLossClaimResponse, Seq(testHateoasLink)))

        val result: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        status(result) shouldBe CREATED
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(CREATED, None, Some(responseBody))
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }
  }

  "handle mdtp validation errors as per spec" when {
    def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockCreateLossClaimRequestDataParser
          .parseRequest(request.CreateLossClaimRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val response: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None)
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }

    val badRequestErrorsFromParser = List(
      BadRequestError,
      NinoFormatError,
      TaxYearClaimedForFormatError.copy(paths = Some(List("/taxYearClaimedFor"))),
      RuleIncorrectOrEmptyBodyError,
      RuleTaxYearNotSupportedError,
      RuleTaxYearRangeInvalid.copy(paths = Some(List("/taxYearClaimedFor"))),
      TypeOfLossFormatError,
      BusinessIdFormatError,
      RuleTypeOfClaimInvalid,
      TypeOfClaimFormatError
    )

    badRequestErrorsFromParser.foreach(errorsFromParserTester(_, BAD_REQUEST))
  }

  "handle non-mdtp validation errors as per spec" when {
    def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockCreateLossClaimRequestDataParser
          .parseRequest(request.CreateLossClaimRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Right(lossClaimRequest))

        MockCreateLossClaimService
          .create(request.CreateLossClaimRequest(Nino(nino), lossClaim))
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val response: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None)
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }

    errorsFromServiceTester(BadRequestError, BAD_REQUEST)
    errorsFromServiceTester(StandardDownstreamError, INTERNAL_SERVER_ERROR)
    errorsFromServiceTester(RuleDuplicateClaimSubmissionError, FORBIDDEN)
    errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
    errorsFromServiceTester(NotFoundError, NOT_FOUND)
    errorsFromServiceTester(RuleTypeOfClaimInvalid, BAD_REQUEST)
    errorsFromServiceTester(RulePeriodNotEnded, FORBIDDEN)
    errorsFromServiceTester(RuleNoAccountingPeriod, FORBIDDEN)
  }
}
