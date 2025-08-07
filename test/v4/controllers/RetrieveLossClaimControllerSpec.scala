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
import common.errors.ClaimIdFormatError
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.Result
import shared.config.Deprecation.NotDeprecated
import shared.config.MockSharedAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.hateoas.Method.GET
import shared.hateoas.{HateoasWrapper, Link, MockHateoasFactory}
import shared.models.domain.Timestamp
import shared.models.errors.*
import shared.models.outcomes.ResponseWrapper
import shared.routing.Version9
import v4.controllers.validators.MockRetrieveLossClaimValidatorFactory
import v4.models.domain.lossClaim.{ClaimId, TypeOfClaim, TypeOfLoss}
import v4.models.request.retrieveLossClaim.RetrieveLossClaimRequestData
import v4.models.response.retrieveLossClaim.{GetLossClaimHateoasData, RetrieveLossClaimResponse}
import v4.services.MockRetrieveLossClaimService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveLossClaimControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSharedAppConfig
    with MockRetrieveLossClaimValidatorFactory
    with MockRetrieveLossClaimService
    with MockHateoasFactory {

  private val claimId      = "AAZZ1234567890a"
  private val businessId   = "XKIS00000000988"
  private val lastModified = Timestamp("2018-07-13T12:13:48.763Z")
  private val taxYear      = "2017-18"

  private val requestData = RetrieveLossClaimRequestData(parsedNino, ClaimId(claimId))

  private val response = RetrieveLossClaimResponse(
    taxYearClaimedFor = taxYear,
    typeOfLoss = TypeOfLoss.`uk-property-non-fhl`,
    businessId = businessId,
    typeOfClaim = TypeOfClaim.`carry-forward`,
    sequence = Some(1),
    lastModified = lastModified
  )

  private val testHateoasLink = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  private val mtdResponseJson = Json.parse(
    s"""
      |{
      |    "taxYearClaimedFor": "$taxYear",
      |    "typeOfLoss": "uk-property-non-fhl",
      |    "typeOfClaim": "carry-forward",
      |    "lastModified": "$lastModified",
      |    "businessId": "$businessId",
      |    "sequence": 1,
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

  "retrieve" should {
    "return UK" when {
      "the request is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockRetrieveLossClaimService
          .retrieve(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrap(response, GetLossClaimHateoasData(validNino, claimId))
          .returns(HateoasWrapper(response, Seq(testHateoasLink)))

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

        MockRetrieveLossClaimService
          .retrieve(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, ClaimIdFormatError, None))))

        runErrorTest(ClaimIdFormatError)
      }
    }
  }

  private trait Test extends ControllerTest {

    val controller: RetrieveLossClaimController = new RetrieveLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockRetrieveLossClaimService,
      validatorFactory = mockRetrieveLossClaimValidatorFactory,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.retrieve(validNino, claimId)(fakeRequest)

    MockedSharedAppConfig.deprecationFor(Version9).returns(NotDeprecated.valid).anyNumberOfTimes()

    MockedSharedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedSharedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false
  }

}
