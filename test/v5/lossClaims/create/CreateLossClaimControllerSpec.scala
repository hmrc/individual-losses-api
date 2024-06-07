/*
 * Copyright 2023 HM Revenue & Customs
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

package v5.lossClaims.create

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.hateoas.Method.GET
import api.hateoas.{HateoasWrapper, Link, MockHateoasFactory}
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import config.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import routing.Version5
import v4.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}
import v5.lossClaims.create.def1.model.request.{Def1_CreateLossClaimRequestBody, Def1_CreateLossClaimRequestData}
import v5.lossClaims.create.def1.model.response.Def1_CreateLossClaimResponse
import v5.lossClaims.create.model.response.CreateLossClaimHateoasData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateLossClaimControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAppConfig
    with MockCreateLossClaimService
    with MockCreateLossClaimValidatorFactory
    with MockHateoasFactory {

  private val lossClaimId = "AAZZ1234567890a"
  private val lossClaim   = Def1_CreateLossClaimRequestBody("2017-18", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-sideways`, "XKIS00000000988")
  private val requestData = Def1_CreateLossClaimRequestData(parsedNino, lossClaim)
  private val createLossClaimResponse = Def1_CreateLossClaimResponse("AAZZ1234567890a")
  private val testHateoasLink         = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

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
        willUseValidator(returningSuccess(requestData))

        MockCreateLossClaimService
          .create(Def1_CreateLossClaimRequestData(parsedNino, lossClaim))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, createLossClaimResponse))))

        MockHateoasFactory
          .wrap(createLossClaimResponse, CreateLossClaimHateoasData(validNino, lossClaimId))
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
      willUseValidator(returning(NinoFormatError))
      runErrorTestWithAudit(NinoFormatError, maybeAuditRequestBody = Some(requestBody))
    }

    "the service returns an error" in new Test {
      willUseValidator(returningSuccess(requestData))

      MockCreateLossClaimService
        .create(Def1_CreateLossClaimRequestData(parsedNino, lossClaim))
        .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTypeOfClaimInvalid, None))))

      runErrorTestWithAudit(RuleTypeOfClaimInvalid, maybeAuditRequestBody = Some(requestBody))
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking {

    private val controller = new CreateLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockCreateLossClaimService,
      validatorFactory = mockCreateLossClaimValidatorFactory,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.create(validNino)(fakePostRequest(requestBody))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateLossClaim",
        transactionName = "create-loss-claim",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = "5.0",
          params = Map("nino" -> validNino),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

    MockedAppConfig.isApiDeprecated(Version5) returns false
  }

}
