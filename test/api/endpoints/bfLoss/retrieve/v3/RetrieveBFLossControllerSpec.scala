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

package api.endpoints.bfLoss.retrieve.v3

import api.controllers.ControllerBaseSpec
import api.endpoints.bfLoss.domain.v3.TypeOfLoss
import api.endpoints.bfLoss.retrieve.v3.request.{ MockRetrieveBFLossParser, RetrieveBFLossRawData, RetrieveBFLossRequest }
import api.endpoints.bfLoss.retrieve.v3.response.{ GetBFLossHateoasData, RetrieveBFLossResponse }
import api.hateoas.MockHateoasFactory
import api.mocks.MockIdGenerator
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{ HateoasWrapper, Link }
import api.services.{ MockEnrolmentsAuthService, MockMtdIdLookupService }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveBFLossControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockRetrieveBFLossService
    with MockRetrieveBFLossParser
    with MockHateoasFactory
    with MockIdGenerator {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  val rawData: RetrieveBFLossRawData = RetrieveBFLossRawData(nino, lossId)
  val request: RetrieveBFLossRequest = RetrieveBFLossRequest(Nino(nino), lossId)

  val response: RetrieveBFLossResponse = RetrieveBFLossResponse(
    taxYearBroughtForwardFrom = "2017-18",
    typeOfLoss = TypeOfLoss.`uk-property-fhl`,
    businessId = "XKIS00000000988",
    lossAmount = 100.00,
    lastModified = "2018-07-13T12:13:48.763Z"
  )

  val testHateoasLink: Link = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  val responseJson: JsValue = Json.parse(
    """
      |{
      |    "businessId": "XKIS00000000988",
      |    "taxYearBroughtForwardFrom": "2017-18",
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
      retrieveBFLossParser = mockRetrieveBFLossParser,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
    MockIdGenerator.getCorrelationId.returns(correlationId)
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
        contentAsJson(result) shouldBe responseJson
        status(result) shouldBe OK
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "handle mdtp validation errors as per spec" when {
      def errorsFromParserTester(error: MtdError): Unit = {
        s"a ${error.code} error is returned from the parser" in new Test {

          MockRetrieveBFLossRequestDataParser
            .parseRequest(rawData)
            .returns(Left(ErrorWrapper(correlationId, error, None)))

          val response: Future[Result] = controller.retrieve(nino, lossId)(fakeRequest)

          contentAsJson(response) shouldBe Json.toJson(error)
          status(response) shouldBe error.httpStatus
          header("X-CorrelationId", response) shouldBe Some(correlationId)
        }
      }

      errorsFromParserTester(NinoFormatError)
      errorsFromParserTester(LossIdFormatError)
      errorsFromParserTester(NotFoundError)
      errorsFromParserTester(BadRequestError)
    }

    "handle non-mdtp validation errors as per spec" when {
      def errorsFromServiceTester(error: MtdError): Unit = {
        s"a ${error.code} error is returned from the service" in new Test {

          MockRetrieveBFLossRequestDataParser
            .parseRequest(rawData)
            .returns(Right(request))

          MockRetrieveBFLossService
            .retrieve(request)
            .returns(Future.successful(Left(ErrorWrapper(correlationId, error, None))))

          val response: Future[Result] = controller.retrieve(nino, lossId)(fakeRequest)
          contentAsJson(response) shouldBe Json.toJson(error)
          status(response) shouldBe error.httpStatus
          header("X-CorrelationId", response) shouldBe Some(correlationId)
        }
      }

      errorsFromServiceTester(NotFoundError)
      errorsFromServiceTester(NinoFormatError)
      errorsFromServiceTester(LossIdFormatError)
      errorsFromServiceTester(BadRequestError)
      errorsFromServiceTester(StandardDownstreamError)
    }
  }
}
