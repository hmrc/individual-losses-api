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

package v3.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.http.HeaderCarrier
import v3.mocks.hateoas.MockHateoasFactory
import v3.mocks.requestParsers.MockAmendLossClaimTypeRequestDataParser
import v3.mocks.services._
import v3.models.domain.{AmendLossClaimTypeRequestBody, Nino, TypeOfClaim, TypeOfLoss}
import v3.models.downstream.{AmendLossClaimTypeHateoasData, LossClaimResponse}
import v3.models.errors._
import v3.models.hateoas.Method.GET
import v3.models.hateoas.{HateoasWrapper, Link}
import v3.models.outcomes.ResponseWrapper
import v3.models.requestData.{AmendLossClaimTypeRawData, AmendLossClaimTypeRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendLossClaimTypeControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAmendLossClaimTypeService
    with MockAmendLossClaimTypeRequestDataParser
    with MockHateoasFactory {

  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino: String = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  val amendLossClaimType: AmendLossClaimTypeRequestBody = AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

  val response: LossClaimResponse =
    LossClaimResponse(
      "2019-20",
      TypeOfLoss.`self-employment`,
      TypeOfClaim.`carry-forward`,
      "XKIS00000000988",
      Some(1),
      "2018-07-13T12:13:48.763Z"
    )

  val request: AmendLossClaimTypeRequest = AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType)

  val testHateoasLink: Link = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  val responseBody: JsValue = Json.parse(
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

  val requestBody: JsValue = Json.parse(
    """
      |{
      |  "typeOfClaim": "carry-forward"
      |}
   """.stripMargin
  )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new AmendLossClaimTypeController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      amendLossClaimTypeService = mockAmendLossClaimTypeService,
      amendLossClaimTypeParser = mockAmendLossClaimTypeRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      cc = cc
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
  }

  "amend" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockAmendLossClaimTypeRequestDataParser.parseRequest(
          AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Right(request))

        MockAmendLossClaimTypeService
          .amend(AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrap(response, AmendLossClaimTypeHateoasData(nino, claimId))
          .returns(HateoasWrapper(response, Seq(testHateoasLink)))

        val result: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))
        contentAsJson(result) shouldBe responseBody
        status(result) shouldBe OK
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "handle mdtp validation errors as per spec" when {
      val badRequestErrorsFromParser = List(
        NinoFormatError,
        RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq("/typeOfClaim"))),
        ClaimIdFormatError,
        TypeOfClaimFormatError
      )

      badRequestErrorsFromParser.foreach(errorsFromParserTester(_, BAD_REQUEST))
    }

    def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockAmendLossClaimTypeRequestDataParser.
          parseRequest(AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val response: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))

        contentAsJson(response) shouldBe Json.toJson(error)
        status(response) shouldBe expectedStatus
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }
  }

  "handle downstream errors as per spec" when {

    errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
    errorsFromServiceTester(ClaimIdFormatError, BAD_REQUEST)
    errorsFromServiceTester(RuleTypeOfClaimInvalid, FORBIDDEN)
    errorsFromServiceTester(RuleClaimTypeNotChanged, FORBIDDEN)
    errorsFromServiceTester(NotFoundError, NOT_FOUND)
    errorsFromServiceTester(DownstreamError, INTERNAL_SERVER_ERROR)

    def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockAmendLossClaimTypeRequestDataParser.parseRequest(
          AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(requestBody)))
          .returns(Right(request))

        MockAmendLossClaimTypeService
          .amend(AmendLossClaimTypeRequest(Nino(nino), claimId, amendLossClaimType))
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val response: Future[Result] = controller.amend(nino, claimId)(fakePostRequest(requestBody))
        contentAsJson(response) shouldBe Json.toJson(error)
        status(response) shouldBe expectedStatus
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }
  }
}