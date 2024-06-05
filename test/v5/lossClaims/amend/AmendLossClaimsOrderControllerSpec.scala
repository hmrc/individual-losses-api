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

package v5.lossClaims.amend

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.hateoas.Method.{GET, PUT}
import api.hateoas.{HateoasWrapper, Link, MockHateoasFactory}
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.TaxYear
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import config.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import routing.Version4
import v4.models.domain.lossClaim.TypeOfClaim
import v5.lossClaims.amendOrder.AmendLossClaimsOrderController
import v5.lossClaims.amendOrder.def1.model.request.{Claim, Def1_AmendLossClaimsOrderRequestBody, Def1_AmendLossClaimsOrderRequestData}
import v5.lossClaims.amendOrder.model.response.{AmendLossClaimsOrderHateoasData, AmendLossClaimsOrderResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendLossClaimsOrderControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAppConfig
    with MockAmendLossClaimsOrderValidatorFactory
    with MockAmendLossClaimsOrderService
    with MockHateoasFactory {

  private val id         = "1234568790ABCDE"
  private val sequence   = 1
  private val taxYear    = "2019-20"
  private val claim      = Claim(id, sequence)
  private val claimsList = Def1_AmendLossClaimsOrderRequestBody(TypeOfClaim.`carry-sideways`, Seq(claim))

  private val requestData =
    Def1_AmendLossClaimsOrderRequestData(parsedNino, TaxYear.fromMtd(taxYear), claimsList)

  private val amendLossClaimsOrderResponse = AmendLossClaimsOrderResponse()

  private val testHateoasLink = Seq(
    Link(href = s"/individuals/losses/$validNino/loss-claims/order/$taxYear", method = PUT, rel = "amend-loss-claim-order"),
    Link(href = s"/individuals/losses/$validNino/loss-claims", method = GET, rel = "list-loss-claims")
  )

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

  private val mtdResponseJson = Json.parse(
    """
      |{
      |   "links":[
      |      {
      |        "href":"/individuals/losses/AA123456A/loss-claims/order/2019-20",
      |        "method":"PUT",
      |        "rel":"amend-loss-claim-order"
      |
      |      },
      |      {
      |        "href":"/individuals/losses/AA123456A/loss-claims",
      |        "method":"GET",
      |        "rel":"list-loss-claims"
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
          .returns(Future.successful(Right(ResponseWrapper(correlationId, amendLossClaimsOrderResponse))))

        MockHateoasFactory
          .wrap(amendLossClaimsOrderResponse, AmendLossClaimsOrderHateoasData(validNino, taxYearClaimedFor = taxYear))
          .returns(HateoasWrapper(amendLossClaimsOrderResponse, testHateoasLink))

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

        MockAmendLossClaimsOrderService
          .amend(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleSequenceOrderBroken, None))))

        runErrorTestWithAudit(RuleSequenceOrderBroken, maybeAuditRequestBody = Some(requestBody))
      }
    }
  }

  private trait Test extends ControllerTest with AuditEventChecking {

    private val controller = new AmendLossClaimsOrderController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockAmendLossClaimsOrderService,
      validatorFactory = mockAmendLossClaimsOrderValidatorFactory,
      hateoasFactory = mockHateoasFactory,
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
          versionNumber = "4.0",
          params = Map("nino" -> validNino, "taxYearClaimedFor" -> taxYear),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

    MockedAppConfig.isApiDeprecated(Version4) returns false
  }

}
