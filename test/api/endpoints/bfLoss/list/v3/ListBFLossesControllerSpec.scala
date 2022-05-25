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

package api.endpoints.bfLoss.list.v3

import api.controllers.ControllerBaseSpec
import api.endpoints.bfLoss.domain.v3.{IncomeSourceType, TypeOfLoss}
import api.endpoints.bfLoss.list.v3
import api.endpoints.bfLoss.list.v3.request.{ListBFLossesRawData, ListBFLossesRequest, MockListBFLossesParser}
import api.endpoints.bfLoss.list.v3.response.{ListBFLossHateoasData, ListBFLossesItem, ListBFLossesResponse}
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.models.domain.{DownstreamTaxYear, Nino}
import api.models.errors._
import api.models.hateoas.Method.{GET, POST}
import api.models.hateoas.{HateoasWrapper, Link}
import api.services.{MockEnrolmentsAuthService, MockMtdIdLookupService}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ListBFLossesControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockListBFLossesService
    with MockListBFLossesParser
    with MockHateoasFactory {

  val correlationId: String  = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino: String           = "AA123456A"
  val taxYear: String        = "2018-19"
  val selfEmployment: String = "self-employment"
  val businessId: String     = "XKIS00000000988"

  val rawData: ListBFLossesRawData = ListBFLossesRawData(nino, Some(taxYear), Some(selfEmployment), Some(businessId))

  val request: ListBFLossesRequest =
    v3.request.ListBFLossesRequest(Nino(nino), Some(DownstreamTaxYear("2019")), Some(IncomeSourceType.`02`), Some(businessId))

  val listHateoasLink: Link = Link(href = "/individuals/losses/TC663795B/brought-forward-losses", method = GET, rel = "self")

  val createHateoasLink: Link =
    Link(href = "/individuals/losses/TC663795B/brought-forward-losses", method = POST, rel = "create-brought-forward-loss")

  val getHateoasLink: String => Link = lossId =>
    Link(href = s"/individuals/losses/TC663795B/brought-forward-losses/$lossId", method = GET, rel = "self")

  // WLOG
  val responseItem: ListBFLossesItem                   = ListBFLossesItem("lossId", "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, "2019-20", "lastModified")
  val response: ListBFLossesResponse[ListBFLossesItem] = ListBFLossesResponse(Seq(responseItem))

  val hateoasResponse: ListBFLossesResponse[HateoasWrapper[ListBFLossesItem]] = ListBFLossesResponse(
    Seq(HateoasWrapper(responseItem, Seq(getHateoasLink("lossId")))))

  val responseJson: JsValue = Json.parse(
    """
      |{
      |  "losses": [
      |    {
      |      "lossId": "lossId",
      |      "businessId": "businessId",
      |      "typeOfLoss": "uk-property-fhl",
      |      "lossAmount": 2.75,
      |      "taxYearBroughtForwardFrom": "2019-20",
      |      "lastModified": "lastModified",
      |      "links": [
      |        {
      |          "href": "/individuals/losses/TC663795B/brought-forward-losses/lossId",
      |          "rel": "self",
      |          "method": "GET"
      |        }
      |      ]
      |    }
      |  ],
      |  "links": [
      |    {
      |      "href": "/individuals/losses/TC663795B/brought-forward-losses",
      |      "rel": "create-brought-forward-loss",
      |      "method": "POST"
      |    },
      |    {
      |      "href": "/individuals/losses/TC663795B/brought-forward-losses",
      |      "rel": "self",
      |      "method": "GET"
      |    }
      |  ]
      |}
    """.stripMargin
  )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new ListBFLossesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      listBFLossesService = mockListBFLossesService,
      listBFLossesParser = mockListBFLossesParser,
      hateoasFactory = mockHateoasFactory,
      cc = cc
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
  }

  "list" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockListBFLossesRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListBFLossesService
          .list(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrapList(response, ListBFLossHateoasData(nino))
          .returns(HateoasWrapper(hateoasResponse, Seq(createHateoasLink, listHateoasLink)))

        val result: Future[Result] = controller.list(nino, Some(taxYear), Some(selfEmployment), Some(businessId))(fakeRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseJson
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return MATCHING_RESOURCE_NOT_FOUND" when {
      "the request received is valid but an empty list of losses is returned from downstream" in new Test {

        MockListBFLossesRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListBFLossesService
          .list(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ListBFLossesResponse(Nil)))))

        MockHateoasFactory
          .wrapList(ListBFLossesResponse(List.empty[ListBFLossesItem]), ListBFLossHateoasData(nino))
          .returns(HateoasWrapper(ListBFLossesResponse(List.empty[HateoasWrapper[ListBFLossesItem]]), Seq(createHateoasLink)))

        val result: Future[Result] = controller.list(nino, Some(taxYear), Some(selfEmployment), Some(businessId))(fakeRequest)
        status(result) shouldBe NOT_FOUND
        contentAsJson(result) shouldBe Json.toJson(NotFoundError)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "handle mdtp validation errors as per spec" when {
      def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the parser" in new Test {

          MockListBFLossesRequestDataParser
            .parseRequest(rawData)
            .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

          val response: Future[Result] = controller.list(nino, Some(taxYear), Some(selfEmployment), Some(businessId))(fakeRequest)

          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
          header("X-CorrelationId", response) shouldBe Some(correlationId)
        }
      }

      errorsFromParserTester(BadRequestError, BAD_REQUEST)
      errorsFromParserTester(NinoFormatError, BAD_REQUEST)
      errorsFromParserTester(TaxYearFormatError, BAD_REQUEST)
      errorsFromParserTester(TypeOfLossFormatError, BAD_REQUEST)
      errorsFromParserTester(BusinessIdFormatError, BAD_REQUEST)
      errorsFromParserTester(RuleTaxYearNotSupportedError, BAD_REQUEST)
      errorsFromParserTester(RuleTaxYearRangeInvalid, BAD_REQUEST)
    }

    "handle non-mdtp validation errors as per spec" when {
      def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the service" in new Test {

          MockListBFLossesRequestDataParser
            .parseRequest(rawData)
            .returns(Right(request))

          MockListBFLossesService
            .list(request)
            .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

          val response: Future[Result] = controller.list(nino, Some(taxYear), Some(selfEmployment), Some(businessId))(fakeRequest)
          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
          header("X-CorrelationId", response) shouldBe Some(correlationId)
        }
      }

      errorsFromServiceTester(BadRequestError, BAD_REQUEST)
      errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
      errorsFromServiceTester(TaxYearFormatError, BAD_REQUEST)
      errorsFromServiceTester(BusinessIdFormatError, BAD_REQUEST)
      errorsFromServiceTester(TypeOfLossFormatError, BAD_REQUEST)
      errorsFromServiceTester(NotFoundError, NOT_FOUND)
      errorsFromServiceTester(StandardDownstreamError, INTERNAL_SERVER_ERROR)
    }
  }
}
