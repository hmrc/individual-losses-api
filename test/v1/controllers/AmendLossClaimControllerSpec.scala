/*
 * Copyright 2019 HM Revenue & Customs
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
import v1.mocks.requestParsers.MockAmendLossClaimRequestDataParser
import v1.mocks.services._
import v1.models.des.LossClaimResponse
import v1.models.domain.{AmendLossClaim, TypeOfClaim, TypeOfLoss}
import v1.models.errors.{NinoFormatError, NotFoundError, RuleIncorrectOrEmptyBodyError, _}
import v1.models.outcomes.DesResponse
import v1.models.requestData.{AmendLossClaimRawData, AmendLossClaimRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendLossClaimControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAmendLossClaimService
    with MockAmendLossClaimRequestDataParser
    with MockAuditService {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino = "AA123456A"
  val claimId = "AAZZ1234567890a"
  val amendLossClaim = AmendLossClaim(TypeOfClaim.`carry-forward`)

  val amendLossClaimResponse =
    LossClaimResponse(Some("XKIS00000000988"), TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, "2019-20", "2018-07-13T12:13:48.763Z")

  val lossClaimRequest: AmendLossClaimRequest = AmendLossClaimRequest(Nino(nino), claimId, amendLossClaim)

  val responseBody: JsValue = Json.parse(
    s"""
      |{
      |    "selfEmploymentId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYear": "2019-20",
      |    "typeOfClaim": "carry-forward",
      |    "lastModified": "2018-07-13T12:13:48.763Z"
      |}
    """.stripMargin)

  val requestBody: JsValue = Json.parse(
    s"""
      |{
      |  "typeOfClaim": "carry-forward"
      |}
    """.stripMargin)

  trait Test {
    val hc = HeaderCarrier()

    val controller = new AmendLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      amendLossClaimService = mockAmendLossClaimService,
      amendLossClaimParser = mockAmendLossClaimRequestDataParser,
      auditService = mockAuditService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "amend" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockAmendLossClaimRequestDataParser.parseRequest(
          AmendLossClaimRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Right(lossClaimRequest))

        MockAmendLossClaimService
          .amend(AmendLossClaimRequest(Nino(nino), claimId, amendLossClaim))
          .returns(Future.successful(Right(DesResponse(correlationId, amendLossClaimResponse))))

        val result: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))
        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBody
      }
    }

    "return single error response with status 400" when {
      "the request received failed the validation" in new Test() {

        MockAmendLossClaimRequestDataParser.parseRequest(
          AmendLossClaimRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(None, NinoFormatError, None)))

        val result: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))
        status(result) shouldBe BAD_REQUEST
        header("X-CorrelationId", result).nonEmpty shouldBe true
      }
    }
    "return a 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        NinoFormatError,
        RuleIncorrectOrEmptyBodyError,
        ClaimIdFormatError,
        TypeOfClaimFormatError
      )

      val badRequestErrorsFromService = List(
        NinoFormatError,
        ClaimIdFormatError
      )

      val notFoundErrorsFromService = List(
        NotFoundError
      )

      val forbiddenErrorsFromService = List(
        RuleClaimTypeNotChanged,
        RuleTypeOfClaimInvalid
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

      MockAmendLossClaimRequestDataParser.
        parseRequest(AmendLossClaimRawData(nino, claimId, AnyContentAsJson(requestBody)))
        .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

      val response: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))

      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
      header("X-CorrelationId", response) shouldBe Some(correlationId)

    }
  }

  def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
    s"a ${error.code} error is returned from the service" in new Test {

      MockAmendLossClaimRequestDataParser.parseRequest(
        AmendLossClaimRawData(nino, claimId, AnyContentAsJson(requestBody)))
        .returns(Right(lossClaimRequest))

      MockAmendLossClaimService
        .amend(AmendLossClaimRequest(Nino(nino), claimId, amendLossClaim))
        .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

      val response: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))
      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
      header("X-CorrelationId", response) shouldBe Some(correlationId)

    }
  }
}
