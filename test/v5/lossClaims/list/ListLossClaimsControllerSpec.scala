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

package v5.lossClaims.list

import cats.implicits.catsSyntaxValidatedId
import common.errors.TypeOfClaimFormatError
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.domain.{BusinessId, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import v5.lossClaims.common.models.TypeOfClaim
import v5.lossClaims.fixtures.ListLossClaimsFixtures.singleClaimResponseModel
import v5.lossClaims.list
import v5.lossClaims.list.def1.request.Def1_ListLossClaimsRequestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ListLossClaimsControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with list.MockListLossClaimsValidatorFactory
    with MockListLossClaimsService {

  private val taxYear        = "2018-19"
  private val selfEmployment = "self-employment"
  private val businessId     = "businessId"
  private val claimType      = "carry-sideways"

  private val requestData =
    Def1_ListLossClaimsRequestData(parsedNino, TaxYear("2019"), None, Some(BusinessId(businessId)), Some(TypeOfClaim.`carry-sideways`))

  private val mtdResponseJson = Json.parse(
    """
      |{
      |    "claims": [
      |        {
      |            "businessId": "testId",
      |            "typeOfClaim": "carry-sideways",
      |            "typeOfLoss": "self-employment",
      |            "taxYearClaimedFor": "2018-19",
      |            "claimId": "claimId",
      |            "sequence": 1,
      |            "lastModified": "2020-07-13T12:13:48.763Z"
      |        }
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

    val controller = new ListLossClaimsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockListLossClaimsService,
      validatorFactory = mockListLossClaimsValidatorFactory,
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

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false
  }

}
