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

package v5.lossClaims.retrieve

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.hateoas.Method.GET
import api.hateoas.{HateoasWrapper, Link, MockHateoasFactory}
import api.models.domain.Timestamp
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import config.MockAppConfig
import play.api.libs.json.Json
import play.api.mvc.Result
import routing.Version4
import v4.models.domain.lossClaim.{ClaimId, TypeOfClaim, TypeOfLoss}
import v5.lossClaims.retrieve.def1.model.request.Def1_RetrieveLossClaimRequestData
import v5.lossClaims.retrieve.def1.model.response.Def1_RetrieveLossClaimResponse
import v5.lossClaims.retrieve.model.response.GetLossClaimHateoasData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveLossClaimControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAppConfig
    with MockRetrieveLossClaimValidatorFactory
    with MockRetrieveLossClaimService
    with MockHateoasFactory {

  private val claimId      = "AAZZ1234567890a"
  private val businessId   = "XKIS00000000988"
  private val lastModified = Timestamp("2018-07-13T12:13:48.763Z")
  private val taxYear      = "2017-18"

  private val requestData = Def1_RetrieveLossClaimRequestData(parsedNino, ClaimId(claimId))

  private val response = Def1_RetrieveLossClaimResponse(
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

    private val controller = new RetrieveLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      service = mockRetrieveLossClaimService,
      validatorFactory = mockRetrieveLossClaimValidatorFactory,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.retrieve(validNino, claimId)(fakeRequest)

    MockedAppConfig.isApiDeprecated(Version4) returns false
  }

}
