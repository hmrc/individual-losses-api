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

package v3.controllers

import api.controllers.ControllerBaseSpec
import api.mocks.hateoas.MockHateoasFactory
import api.mocks.services.{MockEnrolmentsAuthService, MockMtdIdLookupService}
import api.models.domain.Nino
import api.models.errors._
import api.models.hateoas.Method.{GET, POST}
import api.models.hateoas.{HateoasWrapper, Link}
import api.models.outcomes.ResponseWrapper
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import v3.mocks.requestParsers.MockListLossClaimsRequestDataParser
import v3.mocks.services.{MockAuditService, MockListLossClaimsService}
import v3.models.domain.DownstreamTaxYear
import v3.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}
import v3.models.errors._
import v3.models.request.listLossClaims.{ListLossClaimsRawData, ListLossClaimsRequest}
import v3.models.response.listLossClaims.{ListLossClaimsHateoasData, ListLossClaimsItem, ListLossClaimsResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ListLossClaimsControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockListLossClaimsService
    with MockListLossClaimsRequestDataParser
    with MockHateoasFactory
    with MockAuditService {

  val correlationId: String  = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino: String           = "AA123456A"
  val taxYear: String        = "2018-19"
  val selfEmployment: String = "self-employment"
  val businessId: String     = "businessId"
  val claimType: String      = "carry-sideways"

  val rawData: ListLossClaimsRawData = ListLossClaimsRawData(nino, Some(taxYear), Some(selfEmployment), Some(businessId), Some(claimType))

  val request: ListLossClaimsRequest =
    ListLossClaimsRequest(Nino(nino), Some(DownstreamTaxYear("2019")), None, Some(businessId), Some(TypeOfClaim.`carry-sideways`))

  val testHateoasLink: Link       = Link(href = "/foo/bar", method = GET, rel = "test-relationship")
  val testCreateHateoasLink: Link = Link(href = "/foo/bar", method = POST, rel = "test-create-relationship")

  val response: ListLossClaimsResponse[ListLossClaimsItem] = ListLossClaimsResponse(
    Seq(
      ListLossClaimsItem("businessId",
                         TypeOfClaim.`carry-sideways`,
                         TypeOfLoss.`self-employment`,
                         "2020",
                         "claimId",
                         Some(1),
                         "2020-07-13T12:13:48.763Z"),
      ListLossClaimsItem("businessId1",
                         TypeOfClaim.`carry-sideways`,
                         TypeOfLoss.`self-employment`,
                         "2020",
                         "claimId1",
                         Some(2),
                         "2020-07-13T12:13:48.763Z")
    ))

  val hateoasResponse: ListLossClaimsResponse[HateoasWrapper[ListLossClaimsItem]] = ListLossClaimsResponse(
    Seq(
      HateoasWrapper(
        ListLossClaimsItem("XAIS12345678910",
                           TypeOfClaim.`carry-sideways`,
                           TypeOfLoss.`self-employment`,
                           "2020-21",
                           "AAZZ1234567890A",
                           Some(1),
                           "2020-07-13T12:13:48.763Z"),
        Seq(testHateoasLink)
      ),
      HateoasWrapper(
        ListLossClaimsItem("XAIS12345678911",
                           TypeOfClaim.`carry-sideways`,
                           TypeOfLoss.`uk-property-non-fhl`,
                           "2020-21",
                           "AAZZ1234567890B",
                           Some(2),
                           "2020-07-13T12:13:48.763Z"),
        Seq(testHateoasLink)
      )
    ))

  val responseJson: JsValue = Json.parse(
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

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new ListLossClaimsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      listLossClaimsService = mockListLossClaimsService,
      listLossClaimsParser = mockListLossClaimsRequestDataParser,
      mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc
    )

    MockMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockEnrolmentsAuthService.authoriseUser()
  }

  "list" should {
    "return a successful response with header X-CorrelationId and body" when {
      "the request received is valid" in new Test {

        MockListLossClaimsRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListLossClaimsService
          .list(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrapList(response, ListLossClaimsHateoasData(nino))
          .returns(HateoasWrapper(hateoasResponse, Seq(testCreateHateoasLink)))

        val result: Future[Result] = controller.list(nino, Some(taxYear), Some(selfEmployment), Some(businessId), Some(claimType))(fakeRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseJson
      }
    }

    "return MATCHING_RESOURCE_NOT_FOUND" when {
      "the request received is valid but an empty list of claims is returned from downstream" in new Test {

        MockListLossClaimsRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListLossClaimsService
          .list(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ListLossClaimsResponse(Nil)))))

        MockHateoasFactory
          .wrapList(ListLossClaimsResponse(List.empty[ListLossClaimsItem]), ListLossClaimsHateoasData(nino))
          .returns(HateoasWrapper(ListLossClaimsResponse(List.empty[HateoasWrapper[ListLossClaimsItem]]), Seq(testCreateHateoasLink)))

        val result: Future[Result] = controller.list(nino, Some(taxYear), Some(selfEmployment), Some(businessId), Some(claimType))(fakeRequest)
        status(result) shouldBe NOT_FOUND
        contentAsJson(result) shouldBe Json.toJson(NotFoundError)
      }
    }

    "handle mdtp validation errors as per spec" when {
      def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the parser" in new Test {

          MockListLossClaimsRequestDataParser
            .parseRequest(rawData)
            .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

          val response: Future[Result] = controller.list(nino, Some(taxYear), Some(selfEmployment), Some(businessId), Some(claimType))(fakeRequest)

          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
          header("X-CorrelationId", response) shouldBe Some(correlationId)
        }
      }

      errorsFromParserTester(BadRequestError, BAD_REQUEST)
      errorsFromParserTester(NinoFormatError, BAD_REQUEST)
      errorsFromParserTester(TaxYearFormatError, BAD_REQUEST)
      errorsFromParserTester(RuleTaxYearNotSupportedError, BAD_REQUEST)
      errorsFromParserTester(RuleTaxYearRangeInvalid, BAD_REQUEST)
      errorsFromParserTester(TypeOfLossFormatError, BAD_REQUEST)
      errorsFromParserTester(TypeOfClaimFormatError, BAD_REQUEST)
      errorsFromParserTester(BusinessIdFormatError, BAD_REQUEST)
    }

    "handle backend service errors as per spec" when {
      def errorsFromServiceTester(error: MtdError, expectedStatus: Int): Unit = {
        s"a ${error.code} error is returned from the service" in new Test {

          MockListLossClaimsRequestDataParser
            .parseRequest(rawData)
            .returns(Right(request))

          MockListLossClaimsService
            .list(request)
            .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

          val response: Future[Result] = controller.list(nino, Some(taxYear), Some(selfEmployment), Some(businessId), Some(claimType))(fakeRequest)
          status(response) shouldBe expectedStatus
          contentAsJson(response) shouldBe Json.toJson(error)
          header("X-CorrelationId", response) shouldBe Some(correlationId)
        }
      }

      errorsFromServiceTester(BadRequestError, BAD_REQUEST)
      errorsFromServiceTester(NinoFormatError, BAD_REQUEST)
      errorsFromServiceTester(TaxYearFormatError, BAD_REQUEST)
      errorsFromServiceTester(TypeOfLossFormatError, BAD_REQUEST)
      errorsFromServiceTester(BusinessIdFormatError, BAD_REQUEST)
      errorsFromServiceTester(TypeOfClaimFormatError, BAD_REQUEST)
      errorsFromServiceTester(NotFoundError, NOT_FOUND)
      errorsFromServiceTester(StandardDownstreamError, INTERNAL_SERVER_ERROR)
    }
  }
}
