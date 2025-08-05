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
import common.errors.TypeOfClaimFormatError
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.hateoas.HateoasFactory
import shared.models.domain.{BusinessId, TaxYear}
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import v4.controllers.validators.MockListLossClaimsValidatorFactory
import v4.fixtures.ListLossClaimsFixtures.singleClaimResponseModel
import v4.models.domain.lossClaim.TypeOfClaim
import v4.models.request.listLossClaims.ListLossClaimsRequestData
import v4.services.MockListLossClaimsService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ListLossClaimsControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockListLossClaimsValidatorFactory
    with MockListLossClaimsService {

  private val taxYear        = "2018-19"
  private val selfEmployment = "self-employment"
  private val businessId     = "businessId"
  private val claimType      = "carry-sideways"

  private val requestData =
    ListLossClaimsRequestData(parsedNino, TaxYear.fromMtd("2018-19"), None, Some(BusinessId(businessId)), Some(TypeOfClaim.`carry-sideways`))

  private val mtdResponseJson = Json.parse(
    """
      |{
      |  "claims": [
      |    {
      |      "businessId": "testId",
      |      "typeOfClaim": "carry-sideways",
      |      "typeOfLoss": "self-employment",
      |      "taxYearClaimedFor": "2018-19",
      |      "claimId": "claimId",
      |      "sequence": 1,
      |      "lastModified": "2020-07-13T12:13:48.763Z",
      |      "links": [
      |        {
      |          "href": "/individuals/losses/AA123456A/loss-claims/claimId",
      |          "method": "GET",
      |          "rel": "self"
      |        }
      |      ]
      |    }
      |  ],
      |  "links": [
      |    {
      |      "href": "/individuals/losses/AA123456A/loss-claims",
      |      "method": "GET",
      |      "rel": "self"
      |    },
      |    {
      |      "href": "/individuals/losses/AA123456A/loss-claims",
      |      "method": "POST",
      |      "rel": "create-loss-claim"
      |    },
      |    {
      |      "href": "/individuals/losses/AA123456A/loss-claims/order/2018-19",
      |      "method": "PUT",
      |      "rel": "amend-loss-claim-order"
      |    }
      |  ]
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

    val controller: ListLossClaimsController = new ListLossClaimsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockListLossClaimsService,
      validatorFactory = mockListLossClaimsValidatorFactory,
      hateoasFactory = new HateoasFactory(mockSharedAppConfig),
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] =
      controller.list(validNino, taxYear, Some(selfEmployment), Some(businessId), Some(claimType))(fakeRequest)

    MockedSharedAppConfig.deprecationFor(Version9).returns(NotDeprecated.valid).anyNumberOfTimes()

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

  }

}
