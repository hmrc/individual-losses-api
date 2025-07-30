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
import common.errors.RuleDuplicateSubmissionError
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.hateoas.Method.GET
import shared.hateoas.{HateoasWrapper, Link, MockHateoasFactory}
import shared.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import v4.controllers.validators.MockCreateBFLossValidatorFactory
import v4.models.domain.bfLoss.TypeOfLoss
import v4.models.request.createBFLosses.{CreateBFLossRequestBody, CreateBFLossRequestData}
import v4.models.response.createBFLosses.{CreateBFLossHateoasData, CreateBFLossResponse}
import v4.services.MockCreateBFLossService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateBFLossControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with MockCreateBFLossValidatorFactory
    with MockCreateBFLossService
    with MockHateoasFactory {

  private val lossId = "AAZZ1234567890a"

  private val bfLoss               = CreateBFLossRequestBody(TypeOfLoss.`self-employment`, "XKIS00000000988", "2019-20", 256.78)
  private val requestData          = CreateBFLossRequestData(parsedNino, bfLoss)
  private val createBFLossResponse = CreateBFLossResponse("AAZZ1234567890a")
  private val testHateoasLink      = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  private val requestBody: JsValue = Json.parse(
    """
      |{
      |    "businessId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYearBroughtForwardFrom": "2019-20",
      |    "lossAmount": 256.78
      |}
    """.stripMargin
  )

  private val mtdResponseJson: JsValue = Json.parse(
    """
      |{
      |  "lossId": "AAZZ1234567890a",
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
    "return Created" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockCreateBFLossService
          .create(CreateBFLossRequestData(parsedNino, bfLoss))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, createBFLossResponse))))

        MockHateoasFactory
          .wrap(createBFLossResponse, CreateBFLossHateoasData(validNino, lossId))
          .returns(HateoasWrapper(createBFLossResponse, Seq(testHateoasLink)))

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

      MockCreateBFLossService
        .create(CreateBFLossRequestData(parsedNino, bfLoss))
        .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleDuplicateSubmissionError, None))))

      runErrorTestWithAudit(RuleDuplicateSubmissionError, maybeAuditRequestBody = Some(requestBody))
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller: CreateBFLossController = new CreateBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockCreateBFLossService,
      validatorFactory = mockCreateBFLossValidatorFactory,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.create(validNino)(fakePostRequest(requestBody))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateBroughtForwardLoss",
        transactionName = "create-brought-forward-loss",
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

    MockedSharedAppConfig.deprecationFor(Version9).returns(NotDeprecated.valid).anyNumberOfTimes()

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false
  }

}
