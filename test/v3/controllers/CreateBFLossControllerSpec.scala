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
import v3.mocks.requestParsers.MockCreateBFLossRequestDataParser
import v3.mocks.services.{MockCreateBFLossService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v3.models.domain.{BFLoss, Nino, TypeOfLoss}
import v3.models.downstream.{CreateBFLossHateoasData, CreateBFLossResponse}
import v3.models.errors._
import v3.models.hateoas.Method.GET
import v3.models.hateoas.{HateoasWrapper, Link}
import v3.models.outcomes.ResponseWrapper
import v3.models.requestData.{CreateBFLossRawData, CreateBFLossRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateBFLossControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockCreateBFLossService
    with MockCreateBFLossRequestDataParser
    with MockHateoasFactory {

  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino: String = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val bfLoss: BFLoss = BFLoss(TypeOfLoss.`self-employment`, "XKIS00000000988", "2019-20", 256.78)

  val createBFLossResponse: CreateBFLossResponse = CreateBFLossResponse("AAZZ1234567890a")

  val testHateoasLink: Link = Link(href = "/foo/bar", method = GET, rel="test-relationship")

  val bfLossRequest: CreateBFLossRequest = CreateBFLossRequest(Nino(nino), bfLoss)

  val requestBody: JsValue = Json.parse(
    """
      |{
      |    "businessId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYearBroughtForwardFrom": "2019-20",
      |    "lossAmount": 256.78
      |}
    """.stripMargin
  )

  val responseBody: JsValue = Json.parse(
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

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new CreateBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      createBFLossService = mockCreateBFLossService,
      createBFLossParser = mockCreateBFLossRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      cc = cc
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
  }

  "create" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockCreateBFLossRequestDataParser.parseRequest(
          CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Right(bfLossRequest))

        MockCreateBFLossService
          .create(CreateBFLossRequest(Nino(nino), bfLoss))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, createBFLossResponse))))

        MockHateoasFactory
          .wrap(createBFLossResponse, CreateBFLossHateoasData(nino, lossId))
          .returns(HateoasWrapper(createBFLossResponse, Seq(testHateoasLink)))

        val result: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        contentAsJson(result) shouldBe responseBody
        status(result) shouldBe CREATED
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }
  }

  "handle mdtp validation errors as per spec" when {
    def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockCreateBFLossRequestDataParser.
          parseRequest(CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val response: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))

        contentAsJson(response) shouldBe Json.toJson(error)
        status(response) shouldBe expectedStatus
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }

      val badRequestErrorsFromParser = List(
        NinoFormatError,
        TaxYearFormatError.copy(paths = Some(List("/taxYearBroughtForwardFrom"))),
        RuleTaxYearRangeInvalid.copy(paths = Some(List("/taxYearBroughtForwardFrom"))),
        RuleTaxYearNotSupportedError.copy(paths = Some(List("/taxYearBroughtForwardFrom"))),
        ValueFormatError.copy(paths = Some(List("/lossAmount"))),
        BusinessIdFormatError,
        RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/taxYearBroughtForwardFrom"))),
        TypeOfLossFormatError,
        RuleTaxYearNotEndedError
      )

    badRequestErrorsFromParser.foreach(errorsFromParserTester(_, BAD_REQUEST))
  }

  "handle non-mdtp validation errors as per spec" when {
    def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockCreateBFLossRequestDataParser.parseRequest(
          CreateBFLossRawData(nino, AnyContentAsJson(requestBody)))
          .returns(Right(bfLossRequest))

        MockCreateBFLossService
          .create(CreateBFLossRequest(Nino(nino), bfLoss))
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val response: Future[Result] = controller.create(nino)(fakePostRequest(requestBody))
        contentAsJson(response) shouldBe Json.toJson(error)
        status(response) shouldBe expectedStatus
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }

    errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
    errorsFromServiceTester(RuleTaxYearNotEndedError, BAD_REQUEST)
    errorsFromServiceTester(RuleTaxYearNotSupportedError, BAD_REQUEST)
    errorsFromServiceTester(RuleDuplicateSubmissionError, FORBIDDEN)
    errorsFromServiceTester(NotFoundError, NOT_FOUND)
    errorsFromServiceTester(BadRequestError, BAD_REQUEST)
    errorsFromServiceTester(DownstreamError, INTERNAL_SERVER_ERROR)
  }
}
