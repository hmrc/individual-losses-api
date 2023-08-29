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

package v3.controllers

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import api.models.hateoas.Method.{GET, POST}
import api.models.hateoas.{HateoasWrapper, Link}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import v3.controllers.requestParsers.MockListBFLossesRequestParser
import v3.models.domain.bfLoss.{IncomeSourceType, TypeOfLoss}
import v3.models.request.listBFLosses.{ListBFLossesRawData, ListBFLossesRequest}
import v3.models.response.listBFLosses.{ListBFLossHateoasData, ListBFLossesItem, ListBFLossesResponse}
import v3.services.MockListBFLossesService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ListBFLossesControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockListBFLossesService
    with MockListBFLossesRequestParser
    with MockHateoasFactory {

  private val taxYear        = "2018-19"
  private val selfEmployment = "self-employment"
  private val businessId     = "XKIS00000000988"
  private val rawData        = ListBFLossesRawData(nino, Some(taxYear), Some(selfEmployment), Some(businessId))

  private val request =
    ListBFLossesRequest(Nino(nino), Some(TaxYear("2019")), Some(IncomeSourceType.`02`), Some(businessId))

  private val listHateoasLink = Link(href = "/individuals/losses/TC663795B/brought-forward-losses", method = GET, rel = "self")

  private val createHateoasLink =
    Link(href = "/individuals/losses/TC663795B/brought-forward-losses", method = POST, rel = "create-brought-forward-loss")

  private val getHateoasLink: String => Link = lossId =>
    Link(href = s"/individuals/losses/TC663795B/brought-forward-losses/$lossId", method = GET, rel = "self")

  // WLOG
  private val responseItem: ListBFLossesItem = ListBFLossesItem("lossId", "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, "2019-20", "lastModified")
  private val response: ListBFLossesResponse[ListBFLossesItem] = ListBFLossesResponse(Seq(responseItem))

  private val hateoasResponse: ListBFLossesResponse[HateoasWrapper[ListBFLossesItem]] = ListBFLossesResponse(
    Seq(HateoasWrapper(responseItem, Seq(getHateoasLink("lossId")))))

  private val mtdResponseJson: JsValue = Json.parse(
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

  "list" should {
    "return OK" when {
      "the request is valid" in new Test {
        MockListBFLossesRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListBFLossesService
          .list(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrapList(response, ListBFLossHateoasData(nino))
          .returns(HateoasWrapper(hateoasResponse, Seq(createHateoasLink, listHateoasLink)))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        MockListBFLossesRequestDataParser
          .parseRequest(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        MockListBFLossesRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListBFLossesService
          .list(request)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, TypeOfLossFormatError, None))))

        runErrorTest(TypeOfLossFormatError)
      }
    }
  }

  private trait Test extends ControllerTest {

    private val controller = new ListBFLossesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockListBFLossesService,
      parser = mockListBFLossesParser,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.list(nino, Some(taxYear), Some(selfEmployment), Some(businessId))(fakeRequest)
  }

}
