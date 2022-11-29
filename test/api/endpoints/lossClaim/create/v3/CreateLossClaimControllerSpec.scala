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

import api.controllers.{ ControllerBaseSpec, ControllerTestRunner }
import api.endpoints.lossClaim.create.v3.request.{
  CreateLossClaimRawData,
  CreateLossClaimRequest,
  CreateLossClaimRequestBody,
  MockCreateLossClaimParser
}
import api.endpoints.lossClaim.create.v3.response.{ CreateLossClaimHateoasData, CreateLossClaimResponse }
import api.endpoints.lossClaim.domain.v3.{ TypeOfClaim, TypeOfLoss }
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.models.audit.{ AuditEvent, AuditResponse, GenericAuditDetail }
import api.models.domain.Nino
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{ HateoasWrapper, Link }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, Result }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateLossClaimControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockCreateLossClaimService
    with MockCreateLossClaimParser
    with MockHateoasFactory {

  private val lossClaimId             = "AAZZ1234567890a"
  private val lossClaim               = CreateLossClaimRequestBody("2017-18", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-sideways`, "XKIS00000000988")
  private val testHateoasLink         = Link(href = "/foo/bar", method = GET, rel = "test-relationship")
  private val lossClaimRequest        = CreateLossClaimRequest(Nino(nino), lossClaim)
  private val createLossClaimResponse = CreateLossClaimResponse("AAZZ1234567890a")

  private val requestBody = Json.parse(
    """
      |{
      |    "selfEmploymentId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYear": "2017-18",
      |    "typeOfClaim": "carry-forward"
      |}
    """.stripMargin
  )

  private val mtdResponseJson = Json.parse(
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

  "create" should {
    "return CREATED" when {
      "the request is valid" in new Test {
        MockCreateLossClaimRequestDataParser
          .parseRequest(CreateLossClaimRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Right(lossClaimRequest))

        MockCreateLossClaimService
          .create(CreateLossClaimRequest(Nino(nino), lossClaim))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, createLossClaimResponse))))

        MockHateoasFactory
          .wrap(createLossClaimResponse, CreateLossClaimHateoasData(nino, lossClaimId))
          .returns(HateoasWrapper(createLossClaimResponse, Seq(testHateoasLink)))

        runOkTestWithAudit(
          expectedStatus = CREATED,
          maybeAuditRequestBody = Some(requestBody),
          maybeExpectedResponseBody = Some(mtdResponseJson),
          maybeAuditResponseBody = Some(mtdResponseJson)
        )
      }
    }
  }

  "return the error as per spec" when {
    "the parser validation fails" in new Test {
      MockCreateLossClaimRequestDataParser
        .parseRequest(CreateLossClaimRawData(nino, AnyContentAsJson(requestBody)))
        .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

      runErrorTestWithAudit(NinoFormatError, maybeAuditRequestBody = Some(requestBody))
    }

    "the service returns an error" in new Test {
      MockCreateLossClaimRequestDataParser
        .parseRequest(CreateLossClaimRawData(nino, AnyContentAsJson(requestBody)))
        .returns(Right(lossClaimRequest))

      MockCreateLossClaimService
        .create(CreateLossClaimRequest(Nino(nino), lossClaim))
        .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTypeOfClaimInvalid, None))))

      runErrorTestWithAudit(RuleTypeOfClaimInvalid, maybeAuditRequestBody = Some(requestBody))
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking {

    private val controller = new CreateLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      createLossClaimService = mockCreateLossClaimService,
      createLossClaimParser = mockCreateLossClaimParser,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.create(nino)(fakePostRequest(requestBody))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateLossClaim",
        transactionName = "create-loss-claim",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = "3.0",
          params = Map("nino" -> nino),
          request = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          response = auditResponse
        )
      )
  }
}
