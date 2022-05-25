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

import api.controllers.ControllerBaseSpec
import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.endpoints.lossClaim.retrieve.v3.request.{MockRetrieveLossClaimRequestDataParser, RetrieveLossClaimRawData, RetrieveLossClaimRequest}
import api.models.domain.Nino
import api.models.errors._
import api.models.hateoas.Method.GET
import api.models.hateoas.{HateoasWrapper, Link}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import api.endpoints.lossClaim.retrieve.v3
import api.endpoints.lossClaim.retrieve.v3.response.{GetLossClaimHateoasData, RetrieveLossClaimResponse}
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveLossClaimControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockRetrieveLossClaimService
    with MockRetrieveLossClaimRequestDataParser
    with MockHateoasFactory
    with MockAuditService {

  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino: String          = "AA123456A"
  val claimId: String       = "AAZZ1234567890a"
  val businessId            = "XKIS00000000988"
  val lastModified          = "2018-07-13T12:13:48.763Z"
  val taxYear               = "2017-18"

  val rawData: RetrieveLossClaimRawData = RetrieveLossClaimRawData(nino, claimId)
  val request: RetrieveLossClaimRequest = v3.request.RetrieveLossClaimRequest(Nino(nino), claimId)

  val response: RetrieveLossClaimResponse = RetrieveLossClaimResponse(
    taxYearClaimedFor = taxYear,
    typeOfLoss = TypeOfLoss.`uk-property-non-fhl`,
    businessId = businessId,
    typeOfClaim = TypeOfClaim.`carry-forward`,
    lastModified = lastModified,
    sequence = Some(1)
  )

  val testHateoasLink: Link = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  val responseJson: JsValue = Json.parse(
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

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new RetrieveLossClaimController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      retrieveLossClaimService = mockRetrieveLossClaimService,
      retrieveLossClaimParser = mockRetrieveLossClaimRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
  }

  "retrieve" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockRetrieveLossClaimRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockRetrieveLossClaimService
          .retrieve(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrap(response, GetLossClaimHateoasData(nino, claimId))
          .returns(HateoasWrapper(response, Seq(testHateoasLink)))

        val result: Future[Result] = controller.retrieve(nino, claimId)(fakeRequest)
        contentAsJson(result) shouldBe responseJson
        status(result) shouldBe OK
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "handle mdtp validation errors as per spec" when {
      def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the parser" in new Test {

          MockRetrieveLossClaimRequestDataParser
            .parseRequest(rawData)
            .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

          val response: Future[Result] = controller.retrieve(nino, claimId)(fakeRequest)

          contentAsJson(response) shouldBe Json.toJson(error)
          status(response) shouldBe expectedStatus
          header("X-CorrelationId", response) shouldBe Some(correlationId)
        }
      }

      errorsFromParserTester(BadRequestError, BAD_REQUEST)
      errorsFromParserTester(NinoFormatError, BAD_REQUEST)
      errorsFromParserTester(ClaimIdFormatError, BAD_REQUEST)
    }

    "handle non-mdtp validation errors as per spec" when {
      def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the service" in new Test {

          MockRetrieveLossClaimRequestDataParser
            .parseRequest(rawData)
            .returns(Right(request))

          MockRetrieveLossClaimService
            .retrieve(request)
            .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

          val response: Future[Result] = controller.retrieve(nino, claimId)(fakeRequest)
          contentAsJson(response) shouldBe Json.toJson(error)
          status(response) shouldBe expectedStatus
          header("X-CorrelationId", response) shouldBe Some(correlationId)
        }
      }

      errorsFromServiceTester(BadRequestError, BAD_REQUEST)
      errorsFromServiceTester(StandardDownstreamError, INTERNAL_SERVER_ERROR)
      errorsFromServiceTester(NotFoundError, NOT_FOUND)
      errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
      errorsFromServiceTester(ClaimIdFormatError, BAD_REQUEST)
    }
  }
}
