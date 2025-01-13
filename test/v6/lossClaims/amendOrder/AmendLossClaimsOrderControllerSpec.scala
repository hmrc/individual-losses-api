/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.amendOrder

import _root_.common.errors.RuleSequenceOrderBroken
import cats.implicits.catsSyntaxValidatedId
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import shared.models.domain.TaxYear
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import v6.lossClaims.amendOrder.def1.model.request.{Claim, Def1_AmendLossClaimsOrderRequestBody, Def1_AmendLossClaimsOrderRequestData}
import v6.lossClaims.common.models.TypeOfClaim

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendLossClaimsOrderControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with MockAmendLossClaimsOrderValidatorFactory
    with MockAmendLossClaimsOrderService {

  private val id         = "1234568790ABCDE"
  private val sequence   = 1
  private val taxYear    = "2019-20"
  private val claim      = Claim(id, sequence)
  private val claimsList = Def1_AmendLossClaimsOrderRequestBody(TypeOfClaim.`carry-sideways`, Seq(claim))

  private val requestData =
    Def1_AmendLossClaimsOrderRequestData(parsedNino, TaxYear.fromMtd(taxYear), claimsList)

  private val requestBody = Json.parse(
    """
      |{
      |   "claimType": "carry-sideways",
      |   "listOfLossClaims": [
      |      {
      |        "id": "123456789ABCDE",
      |        "sequence":2
      |      },
      |      {
      |        "id": "123456789ABDE0",
      |        "sequence":3
      |      },
      |      {
      |        "id": "123456789ABEF1",
      |        "sequence":1
      |      }
      |   ]
      |}
    """.stripMargin
  )

  "amendLossClaimsOrder" should {
    "return OK" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockAmendLossClaimsOrderService
          .amend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(
          expectedStatus = OK,
          maybeExpectedResponseBody = None,
          maybeAuditRequestBody = Some(requestBody)
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

        MockAmendLossClaimsOrderService
          .amend(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleSequenceOrderBroken, None))))

        runErrorTestWithAudit(RuleSequenceOrderBroken, maybeAuditRequestBody = Some(requestBody))
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller = new AmendLossClaimsOrderController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockAmendLossClaimsOrderService,
      validatorFactory = mockAmendLossClaimsOrderValidatorFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.amendClaimsOrder(validNino, taxYear)(fakePostRequest(requestBody))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "AmendLossClaimOrder",
        transactionName = "amend-loss-claim-order",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = Version9.name,
          params = Map("nino" -> validNino, "taxYearClaimedFor" -> taxYear),
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
