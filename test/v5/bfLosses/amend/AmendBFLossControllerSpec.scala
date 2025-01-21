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

package v5.bfLosses.amend

import cats.implicits.catsSyntaxValidatedId
import common.errors.RuleLossAmountNotChanged
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import shared.models.domain.Timestamp
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import v5.bfLosses.amend.def1.model.request.{Def1_AmendBFLossRequestBody, Def1_AmendBFLossRequestData}
import v5.bfLosses.amend.def1.model.response.Def1_AmendBFLossResponse
import v5.bfLosses.common.domain.{LossId, TypeOfLoss}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendBFLossControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with MockAmendBFLossValidatorFactory
    with MockAmendBFLossService {

  private val lossId       = "AAZZ1234567890a"
  private val parsedLossId = LossId(lossId)
  private val lossAmount   = BigDecimal(2345.67)
  private val amendBFLoss  = Def1_AmendBFLossRequestBody(lossAmount)

  private val amendBFLossResponse = Def1_AmendBFLossResponse(
    businessId = "XBIS12345678910",
    typeOfLoss = TypeOfLoss.`self-employment`,
    lossAmount = lossAmount,
    taxYearBroughtForwardFrom = "2021-22",
    lastModified = Timestamp("2022-07-13T12:13:48.763Z")
  )

  private val requestData = Def1_AmendBFLossRequestData(parsedNino, parsedLossId, amendBroughtForwardLoss = amendBFLoss)

  private val requestBody: JsValue = Json.parse(
    s"""
       |{
       |  "lossAmount": $lossAmount
       |}
    """.stripMargin
  )

  private val mtdResponseJson = Json.parse(
    s"""
       |{
       |    "businessId": "XBIS12345678910",
       |    "typeOfLoss": "self-employment",
       |    "lossAmount": $lossAmount,
       |    "taxYearBroughtForwardFrom": "2021-22",
       |    "lastModified": "2022-07-13T12:13:48.763Z"
       |}
    """.stripMargin
  )

  "amend" should {
    "return OK" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockAmendBFLossService
          .amend(Def1_AmendBFLossRequestData(parsedNino, LossId(lossId), amendBFLoss))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, amendBFLossResponse))))

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

        MockAmendBFLossService
          .amend(Def1_AmendBFLossRequestData(parsedNino, LossId(lossId), amendBFLoss))
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleLossAmountNotChanged))))

        runErrorTestWithAudit(RuleLossAmountNotChanged, maybeAuditRequestBody = Some(requestBody))
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller = new AmendBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockAmendBFLossService,
      validatorFactory = mockAmendBFLossValidatorFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.amend(validNino, lossId)(fakePostRequest(requestBody))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "AmendBroughtForwardLoss",
        transactionName = "amend-brought-forward-loss",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = Version9.name,
          params = Map("nino" -> validNino, "lossId" -> lossId),
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
