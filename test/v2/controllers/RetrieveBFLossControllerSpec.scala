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

package v2.controllers

import api.controllers.ControllerBaseSpec
import api.endpoints.common.lossClaim.v2.domain.TypeOfLoss
import api.mocks.hateoas.MockHateoasFactory
import api.mocks.services.{MockEnrolmentsAuthService, MockMtdIdLookupService}
import api.models.domain.Nino
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{HateoasWrapper, Link}
import api.models.outcomes.ResponseWrapper
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.requestParsers.MockRetrieveBFLossRequestDataParser
import v2.mocks.services.{MockAuditService, MockRetrieveBFLossService}
import v2.models.des.{BFLossResponse, GetBFLossHateoasData}
import v2.models.requestData.{RetrieveBFLossRawData, RetrieveBFLossRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveBFLossControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockRetrieveBFLossService
    with MockRetrieveBFLossRequestDataParser
    with MockHateoasFactory
    with MockAuditService {

  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino: String          = "AA123456A"
  val lossId: String        = "AAZZ1234567890a"

  val rawData: RetrieveBFLossRawData = RetrieveBFLossRawData(nino, lossId)
  val request: RetrieveBFLossRequest = RetrieveBFLossRequest(Nino(nino), lossId)

  val response: BFLossResponse = BFLossResponse(
    taxYear = "2017-18",
    typeOfLoss = TypeOfLoss.`uk-property-fhl`,
    businessId = None,
    lossAmount = 100.00,
    lastModified = "2018-07-13T12:13:48.763Z"
  )

  val testHateoasLink: Link = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  val responseJson: JsValue = Json.parse(
    """
      |{
      |    "taxYear": "2017-18",
      |    "typeOfLoss": "uk-property-fhl",
      |    "lossAmount": 100.00,
      |    "lastModified": "2018-07-13T12:13:48.763Z",
      |    "links": [
      |      {
      |       "href": "/foo/bar",
      |       "method": "GET",
      |       "rel": "test-relationship"
      |      }
      |    ]
      |}
    """.stripMargin
  )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new RetrieveBFLossController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      retrieveBFLossService = mockRetrieveBFLossService,
      retrieveBFLossParser = mockRetrieveBFLossRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
  }

  "retrieve" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockRetrieveBFLossRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockRetrieveBFLossService
          .retrieve(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrap(response, GetBFLossHateoasData(nino, lossId))
          .returns(HateoasWrapper(response, Seq(testHateoasLink)))

        val result: Future[Result] = controller.retrieve(nino, lossId)(fakeRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseJson
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "handle mdtp validation errors as per spec" when {
      def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the parser" in new Test {

          MockRetrieveBFLossRequestDataParser
            .parseRequest(rawData)
            .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

          val response: Future[Result] = controller.retrieve(nino, lossId)(fakeRequest)

          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
          header("X-CorrelationId", response) shouldBe Some(correlationId)
        }
      }

      errorsFromParserTester(BadRequestError, BAD_REQUEST)
      errorsFromParserTester(NotFoundError, NOT_FOUND)
      errorsFromParserTester(NinoFormatError, BAD_REQUEST)
      errorsFromParserTester(LossIdFormatError, BAD_REQUEST)
    }

    "handle non-mdtp validation errors as per spec" when {
      def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the service" in new Test {

          MockRetrieveBFLossRequestDataParser
            .parseRequest(rawData)
            .returns(Right(request))

          MockRetrieveBFLossService
            .retrieve(request)
            .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

          val response: Future[Result] = controller.retrieve(nino, lossId)(fakeRequest)
          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
          header("X-CorrelationId", response) shouldBe Some(correlationId)
        }
      }

      errorsFromServiceTester(BadRequestError, BAD_REQUEST)
      errorsFromServiceTester(StandardDownstreamError, INTERNAL_SERVER_ERROR)
      errorsFromServiceTester(NotFoundError, NOT_FOUND)
      errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
      errorsFromServiceTester(LossIdFormatError, BAD_REQUEST)
    }
  }
}
