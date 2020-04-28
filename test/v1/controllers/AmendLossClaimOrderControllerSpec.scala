/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockAmendLossClaimRequestDataParser
import v1.mocks.services.{MockAmendLossClaimService, MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.audit.{AmendLossClaimAuditDetail, AuditEvent, AuditResponse}
import v1.models.des.{AmendLossClaimHateoasData, ReliefClaimed}
import v1.models.domain.{Claim, LossClaimsList}
import v1.models.hateoas.HateoasWrapper
import v1.models.outcomes.DesResponse
import v1.models.requestData.{AmendLossClaimRawData, AmendLossClaimRequest, AmendLossClaimsOrderRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendLossClaimControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAmendLossClaimService
    with MockAmendLossClaimRequestDataParser
    with MockHateoasFactory
    with MockAuditService {


  val claimType = "carry-sideways"
  val id = "1234568790ABCDE"
  val sequence = 66
  val nino = "AA123456A"
  val taxYear = "01/01/2018"

  val claim = Claim(id, sequence)
  val claimsList = LossClaimsList(ReliefClaimed.`CF`, Seq(claim))
  val amendLossClaimsOrderRequest = AmendLossClaimsOrderRequest(Nino(nino), Some(taxYear),claimsList)

  val requestBody: JsValue = Json.parse(
    """
      |{
      |   "claimType": "carry-sideways",
      |   "listOfLossClaims": [
      |      {
      |      "id": "123456789ABCDE",
      |      "sequence":2
      |      },
      |      {
      |      "id": "123456789ABDE0",
      |      "sequence":3
      |      },
      |      {
      |      "id": "123456789ABEF1",
      |      "sequence":1
      |      }
      |   ]
      |}""".stripMargin)



  trait Test {
    val hc = HeaderCarrier()

    val controller = new AmendLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      amendLossClaimService = mockAmendLossClaimsOrderService,
      amendLossClaimParser = mockAmendLossClaimsOrderRequestDataParser,
      auditService = mockAuditService,
      hateoasFactory = mockHateoasFactory,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "amend" should {
    "return a successful response with header X-CorrelationId and a 204" {
      "the request received is valid" in new Test {


        MockAmendLossClaimRequestDataParser.parseRequest(
          AmendLossClaimRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Right(lossClaimRequest))

        MockAmendLossClaimService
          .amend(AmendLossClaimRequest(Nino(nino), claimId, amendLossClaim))
          .returns(Future.successful(Right(DesResponse(correlationId, amendLossClaimResponse))))

        MockHateoasFactory
          .wrap(NoContent, AmendLossClaimHateoasData(nino, claimId))
          .returns(HateoasWrapper(amendLossClaimResponse, Seq(testHateoasLink)))

        val result: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))
        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val detail = AmendLossClaimAuditDetail(
          "Individual", None, nino,  claimId, requestBody, correlationId,
          AuditResponse(OK, None, Some(responseBody)))
        val event = AuditEvent("amendLossClaim", "amend-loss-claim", detail)
        MockedAuditService.verifyAuditEvent(event).once

      }
    }
  }

}
