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

import api.controllers.ControllerBaseSpec
import api.mocks.hateoas.MockHateoasFactory
import api.mocks.services.{MockEnrolmentsAuthService, MockMtdIdLookupService}
import api.models.domain.Nino
import api.models.domain.v2.TypeOfClaim
import api.models.errors._
import api.models.hateoas.Method.{GET, POST}
import api.models.hateoas.{HateoasWrapper, Link}
import api.models.outcomes.ResponseWrapper
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.requestParsers.MockListLossClaimsRequestDataParser
import v2.mocks.services.{MockAuditService, MockListLossClaimsService}
import v2.models.des.{ListLossClaimsHateoasData, ListLossClaimsResponse, LossClaimId}
import v2.models.errors._
import v2.models.requestData.{DesTaxYear, ListLossClaimsRawData, ListLossClaimsRequest}

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
    ListLossClaimsRequest(Nino(nino), Some(DesTaxYear("2019")), None, Some(businessId), Some(TypeOfClaim.`carry-sideways`))

  val testHateoasLink: Link       = Link(href = "/foo/bar", method = GET, rel = "test-relationship")
  val testCreateHateoasLink: Link = Link(href = "/foo/bar", method = POST, rel = "test-create-relationship")

  val response: ListLossClaimsResponse[LossClaimId] = ListLossClaimsResponse(
    Seq(LossClaimId("000000123456789", Some(1), TypeOfClaim.`carry-sideways`), LossClaimId("000000123456790", Some(2), TypeOfClaim.`carry-sideways`)))

  val hateoasResponse: ListLossClaimsResponse[HateoasWrapper[LossClaimId]] = ListLossClaimsResponse(
    Seq(
      HateoasWrapper(LossClaimId("000000123456789", Some(1), TypeOfClaim.`carry-sideways`), Seq(testHateoasLink)),
      HateoasWrapper(LossClaimId("000000123456790", Some(2), TypeOfClaim.`carry-sideways`), Seq(testHateoasLink))
    ))

  val responseJson: JsValue = Json.parse(
    """
      |{
      |    "claims": [
      |        {
      |            "id": "000000123456789",
      |            "sequence": 1,
      |            "typeOfClaim": "carry-sideways",
      |            "links" : [
      |               {
      |                 "href": "/foo/bar",
      |                 "method": "GET",
      |                 "rel": "test-relationship"
      |               }
      |            ]
      |        },
      |        {
      |            "id": "000000123456790",
      |            "sequence": 2,
      |            "typeOfClaim": "carry-sideways",
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
      "the request received is valid but an empty list of claims is returned from DES" in new Test {

        MockListLossClaimsRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListLossClaimsService
          .list(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ListLossClaimsResponse(Nil)))))

        MockHateoasFactory
          .wrapList(ListLossClaimsResponse(List.empty[LossClaimId]), ListLossClaimsHateoasData(nino))
          .returns(HateoasWrapper(ListLossClaimsResponse(List.empty[HateoasWrapper[LossClaimId]]), Seq(testCreateHateoasLink)))

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
      errorsFromParserTester(ClaimTypeFormatError, BAD_REQUEST)
      errorsFromParserTester(RuleBusinessId, BAD_REQUEST)
      errorsFromParserTester(BusinessIdFormatError, BAD_REQUEST)
    }

    "handle non-mdtp validation errors as per spec" when {
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
      errorsFromServiceTester(RuleBusinessId, BAD_REQUEST)
      errorsFromServiceTester(TypeOfLossFormatError, BAD_REQUEST)
      errorsFromServiceTester(ClaimTypeFormatError, BAD_REQUEST)
      errorsFromServiceTester(NotFoundError, NOT_FOUND)
      errorsFromServiceTester(StandardDownstreamError, INTERNAL_SERVER_ERROR)
    }
  }
}
