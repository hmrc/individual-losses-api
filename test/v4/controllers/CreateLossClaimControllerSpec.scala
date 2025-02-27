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

package v4.controllers

import cats.implicits.catsSyntaxValidatedId
import common.errors.RuleTypeOfClaimInvalid
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.hateoas.Method.GET
import shared.hateoas.{HateoasWrapper, Link, MockHateoasFactory}
import shared.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import v4.controllers.validators.MockCreateLossClaimValidatorFactory
import v4.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}
import v4.models.request.createLossClaim.{CreateLossClaimRequestBody, CreateLossClaimRequestData}
import v4.models.response.createLossClaim.{CreateLossClaimHateoasData, CreateLossClaimResponse}
import v4.services.MockCreateLossClaimService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateLossClaimControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with MockCreateLossClaimService
    with MockCreateLossClaimValidatorFactory
    with MockHateoasFactory {

  private val lossClaimId = "AAZZ1234567890a"
  private val lossClaim   = CreateLossClaimRequestBody("2017-18", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-sideways`, "XKIS00000000988")
  private val requestData = CreateLossClaimRequestData(parsedNino, lossClaim)
  private val createLossClaimResponse = CreateLossClaimResponse("AAZZ1234567890a")
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
          .create(CreateLossClaimRequestData(parsedNino, lossClaim))
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
        .create(CreateLossClaimRequestData(parsedNino, lossClaim))
        .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTypeOfClaimInvalid, None))))

      runErrorTestWithAudit(RuleTypeOfClaimInvalid, maybeAuditRequestBody = Some(requestBody))
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller = new CreateLossClaimController(
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
          versionNumber = Version9.name,
          params = Map("nino" -> validNino),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    MockedSharedAppConfig.deprecationFor(Version9).returns(NotDeprecated.valid).anyNumberOfTimes()
  }

}
