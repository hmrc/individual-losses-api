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

package v4.controllers

import cats.implicits.catsSyntaxValidatedId
import common.errors.TypeOfLossFormatError
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.hateoas.Method.{GET, POST}
import shared.hateoas.{HateoasWrapper, Link, MockHateoasFactory}
import shared.models.domain.{BusinessId, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import v4.controllers.validators.MockListBFLossesValidatorFactory
import v4.models.domain.bfLoss.{IncomeSourceType, TypeOfLoss}
import v4.models.request.listLossClaims.ListBFLossesRequestData
import v4.models.response.listBFLosses.{ListBFLossHateoasData, ListBFLossesItem, ListBFLossesResponse}
import v4.services.MockListBFLossesService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ListBFLossesControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with MockListBFLossesValidatorFactory
    with MockListBFLossesService
    with MockHateoasFactory {

  private val taxYear        = "2018-19"
  private val selfEmployment = "self-employment"
  private val businessId     = "XKIS00000000988"

  private val requestData =
    ListBFLossesRequestData(parsedNino, TaxYear("2019"), Some(IncomeSourceType.`02`), Some(BusinessId(businessId)))

  private val listHateoasLink = Link(href = "/individuals/losses/TC663795B/brought-forward-losses", method = GET, rel = "self")

  private val createHateoasLink =
    Link(href = "/individuals/losses/TC663795B/brought-forward-losses", method = POST, rel = "create-brought-forward-loss")

  private val getHateoasLink: String => Link = lossId =>
    Link(href = s"/individuals/losses/TC663795B/brought-forward-losses/$lossId", method = GET, rel = "self")

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
        willUseValidator(returningSuccess(requestData))

        MockListBFLossesService
          .list(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrapList(response, ListBFLossHateoasData(validNino))
          .returns(HateoasWrapper(hateoasResponse, Seq(createHateoasLink, listHateoasLink)))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))
        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockListBFLossesService
          .list(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, TypeOfLossFormatError, None))))

        runErrorTest(TypeOfLossFormatError)
      }
    }
  }

  private trait Test extends ControllerTest {

    val controller = new ListBFLossesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockListBFLossesService,
      validatorFactory = mockListBFLossesValidatorFactory,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] =
      controller.list(validNino, taxYear, Some(businessId), Some(selfEmployment))(fakeRequest)

    MockedSharedAppConfig.deprecationFor(Version9).returns(NotDeprecated.valid).anyNumberOfTimes()

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

  }

}
