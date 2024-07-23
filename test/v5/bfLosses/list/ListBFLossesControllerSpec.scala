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

package v5.bfLosses.list

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.hateoas.MockHateoasFactory
import api.models.domain.{BusinessId, TaxYear}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import config.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import routing.Version5
import v5.bfLosses.list
import v5.bfLosses.domain._
import v5.bfLosses.list.ListBFLossesController
import v5.bfLosses.list.def1.model.request.Def1_ListBFLossesRequestData
import v5.bfLosses.list.def1.model.response.{Def1_ListBFLossesResponse, ListBFLossesItem}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ListBFLossesControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAppConfig
    with list.MockListBFLossesValidatorFactory
    with list.MockListBFLossesService
    with MockHateoasFactory {

  private val taxYear        = "2018-19"
  private val selfEmployment = "self-employment"
  private val businessId     = "XKIS00000000988"

  private val requestData =
    Def1_ListBFLossesRequestData(parsedNino, TaxYear("2019"), Some(IncomeSourceType.`02`), Some(BusinessId(businessId)))

  private val responseItem: ListBFLossesItem = ListBFLossesItem("lossId", "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, "2019-20", "lastModified")
  private val response: Def1_ListBFLossesResponse = Def1_ListBFLossesResponse(Seq(responseItem))

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
      |      "lastModified": "lastModified"
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

    private val controller = new ListBFLossesController(
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

    MockedAppConfig.isApiDeprecated(Version5) returns false
  }

}
