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
import v1.mocks.requestParsers.MockAmendLossClaimsOrderRequestDataParser
import v1.mocks.services.{MockAmendLossClaimsOrderService, MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.des.{AmendLossClaimsOrderHateoasData, AmendLossClaimsOrderResponse, ReliefClaimed}
import v1.models.domain.{Claim, LossClaimsList}
import v1.models.errors._
import v1.models.hateoas.Method.GET
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.outcomes.DesResponse
import v1.models.requestData.{AmendLossClaimsOrderRawData, AmendLossClaimsOrderRequest, DesTaxYear}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendLossClaimsOrderControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAmendLossClaimsOrderService
    with MockAmendLossClaimsOrderRequestDataParser
    with MockHateoasFactory
    with MockAuditService {


  val claimType = "carry-sideways"
  val id = "1234568790ABCDE"
  val sequence = 1
  val nino = "AA123456A"
  val taxYear = "2019-20"
  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val claim = Claim(id, sequence)
  val claimsList = LossClaimsList(ReliefClaimed.`CF`, Seq(claim))
  val amendLossClaimsOrderRequest = AmendLossClaimsOrderRequest(Nino(nino), DesTaxYear.fromMtd(taxYear), claimsList)
  val amendLossClaimsOrderResponse = AmendLossClaimsOrderResponse()

  val testHateoasLink = Link(href = s"/individuals/losses/${nino}/loss-claims/order", method = GET, rel = "self")

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

  val responseBody: JsValue = Json.parse(
    """
      |{
      |"links":[
      |          {
      |               "href":"/individuals/losses/AA123456A/loss-claims/order",
      |               "rel":"self",
      |               "method":"GET"
      |            }
      |         ]
      |}
      |""".stripMargin
  )


  trait Test {
    val hc = HeaderCarrier()

    val controller = new AmendLossClaimsOrderController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      amendLossClaimsOrderService = mockAmendLossClaimsOrderService,
      amendLossClaimsOrderParser = mockAmendLossClaimsRequestDataParser,
      auditService = mockAuditService,
      hateoasFactory = mockHateoasFactory,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "amendLossClaimsOrder" should {
    "return a successful response with header X-CorrelationId and a 200" when {
      "the request received is valid" in new Test {

        MockAmendLossClaimsOrderRequestDataParser.parseRequest(
          AmendLossClaimsOrderRawData(nino, Some(taxYear), AnyContentAsJson(requestBody)))
          .returns(Right(amendLossClaimsOrderRequest))

        MockAmendLossClaimsOrderService
          .amend(amendLossClaimsOrderRequest)
          .returns(Future.successful(Right(DesResponse(correlationId, amendLossClaimsOrderResponse))))

        MockHateoasFactory
          .wrap(amendLossClaimsOrderResponse, AmendLossClaimsOrderHateoasData(nino))
          .returns(HateoasWrapper(amendLossClaimsOrderResponse, Seq(testHateoasLink)))

        val result: Future[Result] = controller.amendClaimsOrder(nino, Some(taxYear))(fakePostRequest(requestBody))
        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return a 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        BadRequestError,
        NinoFormatError,
        TaxYearFormatError,
        RuleIncorrectOrEmptyBodyError,
        ClaimTypeFormatError,
        ClaimIdFormatError,
        SequenceFormatError,
        RuleInvalidSequenceStart,
        RuleSequenceOrderBroken
      )

      val badRequestErrorsFromService = List(
        NinoFormatError,
        RuleSequenceOrderBroken,
        RuleLossClaimsMissing
      )

      val notFoundErrorsFromService = List(
        NotFoundError
      )

      val forbiddenErrorsFromService = List(
        UnauthorisedError
      )

      badRequestErrorsFromParser.foreach(errorsFromParserTester(_, BAD_REQUEST))
      badRequestErrorsFromService.foreach(errorsFromServiceTester(_, BAD_REQUEST))
      notFoundErrorsFromService.foreach(errorsFromServiceTester(_, NOT_FOUND))
      forbiddenErrorsFromService.foreach(errorsFromServiceTester(_, FORBIDDEN))
      errorsFromServiceTester(DownstreamError, INTERNAL_SERVER_ERROR)
    }
  }

    def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockAmendLossClaimsOrderRequestDataParser.parseRequest(AmendLossClaimsOrderRawData(nino, Some(taxYear), AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val response: Future[Result] = controller.amendClaimsOrder(nino, Some(taxYear))(fakePostRequest(requestBody))

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }


    def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockAmendLossClaimsOrderRequestDataParser.parseRequest(
          AmendLossClaimsOrderRawData(nino, Some(taxYear), AnyContentAsJson(requestBody)))
          .returns(Right(amendLossClaimsOrderRequest))

        MockAmendLossClaimsOrderService
          .amend(amendLossClaimsOrderRequest)
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val response: Future[Result] = controller.amendClaimsOrder(nino, Some(taxYear))(fakePostRequest(requestBody))
        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }
  }
