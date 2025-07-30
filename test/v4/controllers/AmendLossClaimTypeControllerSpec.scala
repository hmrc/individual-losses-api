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
import shared.models.domain.Timestamp
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import v4.controllers.validators.MockAmendLossClaimTypeValidatorFactory
import v4.models.domain.lossClaim.{ClaimId, TypeOfClaim, TypeOfLoss}
import v4.models.request.amendLossClaimType.{AmendLossClaimTypeRequestBody, AmendLossClaimTypeRequestData}
import v4.models.response.amendLossClaimType.{AmendLossClaimTypeHateoasData, AmendLossClaimTypeResponse}
import v4.services.MockAmendLossClaimTypeService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendLossClaimTypeControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with MockAmendLossClaimTypeValidatorFactory
    with MockAmendLossClaimTypeService
    with MockHateoasFactory {

  private val claimId            = ClaimId("AAZZ1234567890a")
  private val amendLossClaimType = AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

  private val requestData = AmendLossClaimTypeRequestData(parsedNino, claimId, amendLossClaimType)

  private val amendLossClaimTypeResponse =
    AmendLossClaimTypeResponse(
      "2019-20",
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      "XKIS00000000988",
      Some(1),
      Timestamp("2018-07-13T12:13:48.763Z")
    )

  private val testHateoasLink = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  private val mtdResponseJson = Json.parse(
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

  private val requestBody = Json.parse(
    """
      |{
      |  "typeOfClaim": "carry-forward"
      |}
   """.stripMargin
  )

  "amendLossClaimType" should {
    "return OK" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockAmendLossClaimTypeService
          .amend(AmendLossClaimTypeRequestData(parsedNino, claimId, amendLossClaimType))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, amendLossClaimTypeResponse))))

        MockHateoasFactory
          .wrap(amendLossClaimTypeResponse, AmendLossClaimTypeHateoasData(validNino, claimId.claimId))
          .returns(HateoasWrapper(amendLossClaimTypeResponse, Seq(testHateoasLink)))

        runOkTestWithAudit(
          expectedStatus = OK,
          maybeExpectedResponseBody = Some(mtdResponseJson),
          maybeAuditRequestBody = Some(requestBody),
          maybeAuditResponseBody = Some(mtdResponseJson)
        )
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))
        runErrorTestWithAudit(NinoFormatError, maybeAuditRequestBody = Some(requestBody))
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockAmendLossClaimTypeService
          .amend(AmendLossClaimTypeRequestData(parsedNino, claimId, amendLossClaimType))
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTypeOfClaimInvalid, None))))

        runErrorTestWithAudit(RuleTypeOfClaimInvalid, maybeAuditRequestBody = Some(requestBody))
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller: AmendLossClaimTypeController = new AmendLossClaimTypeController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockAmendLossClaimTypeService,
      validatorFactory = mockAmendLossClaimTypeValidatorFactory,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.amend(validNino, claimId.claimId)(fakePostRequest(requestBody))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "AmendLossClaim",
        transactionName = "amend-loss-claim",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = Version9.name,
          params = Map("nino" -> validNino, "claimId" -> claimId.claimId),
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
