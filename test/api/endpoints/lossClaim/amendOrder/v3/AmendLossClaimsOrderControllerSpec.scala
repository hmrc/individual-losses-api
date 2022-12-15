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

package api.endpoints.lossClaim.amendOrder.v3

import api.controllers.{ ControllerBaseSpec, ControllerTestRunner }
import api.endpoints.lossClaim.amendOrder.v3.model.Claim
import api.endpoints.lossClaim.amendOrder.v3.request.{
  AmendLossClaimsOrderRawData,
  AmendLossClaimsOrderRequest,
  AmendLossClaimsOrderRequestBody,
  MockAmendLossClaimsOrderRequestDataParser
}
import api.endpoints.lossClaim.amendOrder.v3.response.{ AmendLossClaimsOrderHateoasData, AmendLossClaimsOrderResponse }
import api.endpoints.lossClaim.domain.v3.TypeOfClaim
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.models.audit.{ AuditEvent, AuditResponse, GenericAuditDetail }
import api.models.domain.{ Nino, TaxYear }
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{ HateoasWrapper, Link }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsJson, Result }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendLossClaimsOrderControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAmendLossClaimsOrderService
    with MockAmendLossClaimsOrderRequestDataParser
    with MockHateoasFactory {

  private val id         = "1234568790ABCDE"
  private val sequence   = 1
  private val taxYear    = "2019-20"
  private val claim      = Claim(id, sequence)
  private val claimsList = AmendLossClaimsOrderRequestBody(TypeOfClaim.`carry-sideways`, Seq(claim))

  private val amendLossClaimsOrderRequest =
    AmendLossClaimsOrderRequest(Nino(nino), TaxYear.fromMtd(taxYear), claimsList)
  private val amendLossClaimsOrderResponse = AmendLossClaimsOrderResponse()

  private val testHateoasLink = Link(href = s"/individuals/losses/$nino/loss-claims/order", method = GET, rel = "self")

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
      |        "href":"/individuals/losses/AA123456A/loss-claims/order",
      |        "rel":"self",
      |        "method":"GET"
      |      }
      |   ]
      |}
    """.stripMargin
  )

  "amendLossClaimsOrder" should {
    "return OK" when {
      "the request is valid" in new Test {
        MockAmendLossClaimsOrderRequestDataParser
          .parseRequest(AmendLossClaimsOrderRawData(nino, taxYear, AnyContentAsJson(requestBody)))
          .returns(Right(amendLossClaimsOrderRequest))

        MockAmendLossClaimsOrderService
          .amend(amendLossClaimsOrderRequest)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, amendLossClaimsOrderResponse))))

        MockHateoasFactory
          .wrap(amendLossClaimsOrderResponse, AmendLossClaimsOrderHateoasData(nino))
          .returns(HateoasWrapper(amendLossClaimsOrderResponse, Seq(testHateoasLink)))

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
        MockAmendLossClaimsOrderRequestDataParser
          .parseRequest(AmendLossClaimsOrderRawData(nino, taxYear, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

        runErrorTestWithAudit(NinoFormatError, maybeAuditRequestBody = Some(requestBody))
      }

      "the service returns an error" in new Test {
        MockAmendLossClaimsOrderRequestDataParser
          .parseRequest(AmendLossClaimsOrderRawData(nino, taxYear, AnyContentAsJson(requestBody)))
          .returns(Right(amendLossClaimsOrderRequest))

        MockAmendLossClaimsOrderService
          .amend(amendLossClaimsOrderRequest)
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
      parser = mockAmendLossClaimsRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.amendClaimsOrder(nino, taxYear)(fakePostRequest(requestBody))

    protected def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "AmendLossClaimOrder",
        transactionName = "amend-loss-claim-order",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          versionNumber = "3.0",
          params = Map("nino" -> nino, "taxYearClaimedFor" -> taxYear),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )
  }

}
