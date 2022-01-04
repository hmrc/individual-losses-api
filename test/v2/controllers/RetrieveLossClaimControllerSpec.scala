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

package v2.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.hateoas.MockHateoasFactory
import v2.mocks.requestParsers.MockRetrieveLossClaimRequestDataParser
import v2.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService, MockRetrieveLossClaimService}
import v2.models.des.{GetLossClaimHateoasData, LossClaimResponse}
import v2.models.domain.{Nino, TypeOfClaim, TypeOfLoss}
import v2.models.errors.{NotFoundError, _}
import v2.models.hateoas.Method.GET
import v2.models.hateoas.{HateoasWrapper, Link}
import v2.models.outcomes.DesResponse
import v2.models.requestData.{RetrieveLossClaimRawData, RetrieveLossClaimRequest}

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
  val nino: String = "AA123456A"
  val claimId: String = "AAZZ1234567890a"

  val rawData: RetrieveLossClaimRawData = RetrieveLossClaimRawData(nino, claimId)
  val request: RetrieveLossClaimRequest = RetrieveLossClaimRequest(Nino(nino), claimId)

  val response: LossClaimResponse = LossClaimResponse(
    taxYear = "2017-18",
    typeOfLoss = TypeOfLoss.`uk-property-fhl`,
    businessId = None,
    typeOfClaim = TypeOfClaim.`carry-forward`,
    lastModified = "2018-07-13T12:13:48.763Z"
  )

  val testHateoasLink: Link = Link(href = "/foo/bar", method = GET, rel = "test-relationship")

  val responseJson: JsValue = Json.parse(
    """
      |{
      |    "taxYear": "2017-18",
      |    "typeOfLoss": "uk-property-fhl",
      |    "typeOfClaim": "carry-forward",
      |    "lastModified": "2018-07-13T12:13:48.763Z",
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
          .returns(Future.successful(Right(DesResponse(correlationId, response))))

        MockHateoasFactory
          .wrap(response, GetLossClaimHateoasData(nino, claimId))
          .returns(HateoasWrapper(response, Seq(testHateoasLink)))

        val result: Future[Result] = controller.retrieve(nino, claimId)(fakeRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseJson
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

          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
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
          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
          header("X-CorrelationId", response) shouldBe Some(correlationId)
        }
      }

      errorsFromServiceTester(BadRequestError, BAD_REQUEST)
      errorsFromServiceTester(DownstreamError, INTERNAL_SERVER_ERROR)
      errorsFromServiceTester(NotFoundError, NOT_FOUND)
      errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
      errorsFromServiceTester(ClaimIdFormatError, BAD_REQUEST)
    }
  }
}