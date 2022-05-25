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

package api.endpoints.lossClaim.amendType.v3

import api.controllers.ControllerBaseSpec
import api.endpoints.lossClaim.amendType.v3
import api.endpoints.lossClaim.amendType.v3.request.{AmendLossClaimTypeRawData, AmendLossClaimTypeRequest, AmendLossClaimTypeRequestBody, MockAmendLossClaimTypeRequestDataParser}
import api.endpoints.lossClaim.amendType.v3.response.{AmendLossClaimTypeHateoasData, AmendLossClaimTypeResponse}
import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.models.audit.{AuditError, AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.Nino
import api.models.errors._
import api.models.errors.v3.{RuleClaimTypeNotChanged, RuleTypeOfClaimInvalid}
import api.models.hateoas.Method.GET
import api.models.hateoas.{HateoasWrapper, Link}
import api.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.http.HeaderCarrier

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
  val nino: String          = "AA123456A"
  val claimId: String       = "AAZZ1234567890a"

  val amendLossClaimType: AmendLossClaimTypeRequestBody = AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

  val response: AmendLossClaimTypeResponse =
    AmendLossClaimTypeResponse(
      "2019-20",
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      "XKIS00000000988",
      Some(1),
      "2018-07-13T12:13:48.763Z"
    )

  val request: AmendLossClaimTypeRequest = v3.request.AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType)

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

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "AmendLossClaim",
      transactionName = "amend-loss-claim",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        versionNumber = "3.0",
        params = Map("nino" -> nino, "claimId" -> claimId),
        request = Some(requestBody),
        `X-CorrelationId` = correlationId,
        response = auditResponse
      )
    )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new AmendLossClaimTypeController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      amendLossClaimTypeService = mockAmendLossClaimTypeService,
      amendLossClaimTypeParser = mockAmendLossClaimTypeRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
  }

  "amend" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockAmendLossClaimTypeRequestDataParser
          .parseRequest(AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Right(request))

        MockAmendLossClaimTypeService
          .amend(v3.request.AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrap(response, AmendLossClaimTypeHateoasData(nino, claimId))
          .returns(HateoasWrapper(response, Seq(testHateoasLink)))

        val result: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))
        contentAsJson(result) shouldBe responseBody
        status(result) shouldBe OK
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(OK, None, Some(responseBody))
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }

    "handle mdtp validation errors as per spec" when {
      val badRequestErrorsFromParser = List(
        NinoFormatError,
        RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq("/typeOfClaim"))),
        ClaimIdFormatError,
        TypeOfClaimFormatError
      )

      badRequestErrorsFromParser.foreach(errorsFromParserTester(_, BAD_REQUEST))
    }

    def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockAmendLossClaimTypeRequestDataParser
          .parseRequest(v3.request.AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val response: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))

        contentAsJson(response) shouldBe Json.toJson(error)
        status(response) shouldBe expectedStatus
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None)
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }
  }

  "handle downstream errors as per spec" when {

    errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
    errorsFromServiceTester(ClaimIdFormatError, BAD_REQUEST)
    errorsFromServiceTester(RuleTypeOfClaimInvalid, FORBIDDEN)
    errorsFromServiceTester(RuleClaimTypeNotChanged, FORBIDDEN)
    errorsFromServiceTester(NotFoundError, NOT_FOUND)
    errorsFromServiceTester(StandardDownstreamError, INTERNAL_SERVER_ERROR)

    def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockAmendLossClaimTypeRequestDataParser
          .parseRequest(v3.request.AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Right(request))

        MockAmendLossClaimTypeService
          .amend(v3.request.AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType))
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val response: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))
        contentAsJson(response) shouldBe Json.toJson(error)
        status(response) shouldBe expectedStatus
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None)
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }
  }
}
