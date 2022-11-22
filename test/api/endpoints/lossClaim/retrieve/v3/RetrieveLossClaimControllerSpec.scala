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

package api.endpoints.lossClaim.retrieve.v3

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.endpoints.lossClaim.retrieve.v3.request.{MockRetrieveLossClaimRequestDataParser, RetrieveLossClaimRawData, RetrieveLossClaimRequest}
import api.endpoints.lossClaim.retrieve.v3.response.{GetLossClaimHateoasData, RetrieveLossClaimResponse}
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.models.domain.Nino
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{HateoasWrapper, Link}
import play.api.libs.json.Json
import play.api.mvc.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveLossClaimControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockRetrieveLossClaimService
    with MockRetrieveLossClaimRequestDataParser
    with MockHateoasFactory {

  private val claimId      = "AAZZ1234567890a"
  private val businessId   = "XKIS00000000988"
  private val lastModified = "2018-07-13T12:13:48.763Z"
  private val taxYear      = "2017-18"

  private val rawData = RetrieveLossClaimRawData(nino, claimId)
  private val request = RetrieveLossClaimRequest(Nino(nino), claimId)

  private val response = RetrieveLossClaimResponse(
    taxYearClaimedFor = taxYear,
    typeOfLoss = TypeOfLoss.`uk-property-non-fhl`,
    businessId = businessId,
    typeOfClaim = TypeOfClaim.`carry-forward`,
    lastModified = lastModified,
    sequence = Some(1)
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
        MockRetrieveLossClaimRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockRetrieveLossClaimService
          .retrieve(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrap(response, GetLossClaimHateoasData(nino, claimId))
            .returns(HateoasWrapper(response, Seq(testHateoasLink)))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        MockRetrieveLossClaimRequestDataParser
          .parseRequest(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        MockRetrieveLossClaimRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockRetrieveLossClaimService
          .retrieve(request)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, ClaimIdFormatError, None))))

        runErrorTest(ClaimIdFormatError)
      }
    }
  }

  private trait Test extends ControllerTest {

    private val controller = new RetrieveLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      retrieveLossClaimService = mockRetrieveLossClaimService,
      retrieveLossClaimParser = mockRetrieveLossClaimRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.retrieve(nino, claimId)(fakeRequest)
  }
}
