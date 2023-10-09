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

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.hateoas.Method.{GET, POST}
import api.hateoas.{HateoasWrapper, Link, MockHateoasFactory}
import api.models.domain.{BusinessId, TaxYear}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import config.MockAppConfig
import play.api.libs.json.Json
import play.api.mvc.Result
import routing.Version4
import v4.controllers.validators.MockListLossClaimsValidatorFactory
import v4.fixtures.ListLossClaimsFixtures.singleClaimResponseModel
import v4.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}
import v4.models.request.listLossClaims.ListLossClaimsRequestData
import v4.models.response.listLossClaims.{ListLossClaimsHateoasData, ListLossClaimsItem, ListLossClaimsResponse}
import v4.services.MockListLossClaimsService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ListLossClaimsControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAppConfig
    with MockListLossClaimsValidatorFactory
    with MockListLossClaimsService
    with MockHateoasFactory {

  private val taxYear        = "2018-19"
  private val selfEmployment = "self-employment"
  private val businessId     = "businessId"
  private val claimType      = "carry-sideways"

  private val requestData =
    ListLossClaimsRequestData(parsedNino, TaxYear("2019"), None, Some(BusinessId(businessId)), Some(TypeOfClaim.`carry-sideways`))

  private val testHateoasLink       = Link(href = "/foo/bar", method = GET, rel = "test-relationship")
  private val testCreateHateoasLink = Link(href = "/foo/bar", method = POST, rel = "test-create-relationship")

  private val hateoasResponse: ListLossClaimsResponse[HateoasWrapper[ListLossClaimsItem]] = ListLossClaimsResponse(
    List(
      HateoasWrapper(
        ListLossClaimsItem(
          "XAIS12345678910",
          TypeOfClaim.`carry-sideways`,
          TypeOfLoss.`self-employment`,
          "2020-21",
          "AAZZ1234567890A",
          Some(1),
          "2020-07-13T12:13:48.763Z"),
        List(testHateoasLink)
      ),
      HateoasWrapper(
        ListLossClaimsItem(
          "XAIS12345678911",
          TypeOfClaim.`carry-sideways`,
          TypeOfLoss.`uk-property-non-fhl`,
          "2020-21",
          "AAZZ1234567890B",
          Some(2),
          "2020-07-13T12:13:48.763Z"),
        List(testHateoasLink)
      )
    ))

  private val mtdResponseJson = Json.parse(
    """
      |{
      |    "claims": [
      |        {
      |            "businessId": "XAIS12345678910",
      |            "typeOfClaim": "carry-sideways",
      |            "typeOfLoss": "self-employment",
      |            "taxYearClaimedFor": "2020-21",
      |            "claimId": "AAZZ1234567890A",
      |            "sequence": 1,
      |            "lastModified": "2020-07-13T12:13:48.763Z",
      |            "links" : [
      |               {
      |                 "href": "/foo/bar",
      |                 "method": "GET",
      |                 "rel": "test-relationship"
      |               }
      |            ]
      |        },
      |        {
      |            "businessId": "XAIS12345678911",
      |            "typeOfClaim": "carry-sideways",
      |            "typeOfLoss": "uk-property-non-fhl",
      |            "taxYearClaimedFor": "2020-21",
      |            "claimId": "AAZZ1234567890B",
      |            "sequence": 2,
      |            "lastModified": "2020-07-13T12:13:48.763Z",
      |            "links" : [
      |               {
      |                 "href": "/foo/bar",
      |                 "method": "GET",
      |                 "rel": "test-relationship"
      |               }
      |            ]
      |        }
      |    ],
      |    "links" : [
      |       {
      |         "href": "/foo/bar",
      |         "method": "POST",
      |         "rel": "test-create-relationship"
      |       }
      |    ]
      |}
    """.stripMargin
  )

  "list" should {
    "return OK" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockListLossClaimsService
          .list(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, singleClaimResponseModel(taxYear)))))

        MockHateoasFactory
          .wrapList(singleClaimResponseModel(taxYear), ListLossClaimsHateoasData(validNino, taxYearClaimedFor = taxYear))
          .returns(HateoasWrapper(hateoasResponse, List(testCreateHateoasLink)))

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

        MockListLossClaimsService
          .list(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, TypeOfClaimFormatError, None))))

        runErrorTest(TypeOfClaimFormatError)
      }
    }
  }

  private trait Test extends ControllerTest {

    private val controller = new ListLossClaimsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockListLossClaimsService,
      validatorFactory = mockListLossClaimsValidatorFactory,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] =
      controller.list(validNino, taxYear, Some(selfEmployment), Some(businessId), Some(claimType))(fakeRequest)

    MockedAppConfig.isApiDeprecated(Version4) returns false
  }

}
