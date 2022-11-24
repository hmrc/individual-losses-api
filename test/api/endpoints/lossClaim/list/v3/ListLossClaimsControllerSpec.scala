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

package api.endpoints.lossClaim.list.v3

import api.controllers.{ ControllerBaseSpec, ControllerTestRunner }
import api.endpoints.lossClaim.domain.v3.{ TypeOfClaim, TypeOfLoss }
import api.endpoints.lossClaim.list.v3.request.{ ListLossClaimsRawData, ListLossClaimsRequest, MockListLossClaimsRequestDataParser }
import api.endpoints.lossClaim.list.v3.response.{ ListLossClaimsHateoasData, ListLossClaimsItem, ListLossClaimsResponse }
import api.hateoas.MockHateoasFactory
import api.models.ResponseWrapper
import api.models.domain.{ Nino, TaxYear }
import api.models.errors._
import api.models.hateoas.Method.{ GET, POST }
import api.models.hateoas.{ HateoasWrapper, Link }
import play.api.libs.json.Json
import play.api.mvc.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ListLossClaimsControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockListLossClaimsService
    with MockListLossClaimsRequestDataParser
    with MockHateoasFactory {

  private val selfEmployment = "self-employment"
  private val businessId     = "businessId"
  private val claimType      = "carry-sideways"

  private val testHateoasLink       = Link(href = "/foo/bar", method = GET, rel = "test-relationship")
  private val testCreateHateoasLink = Link(href = "/foo/bar", method = POST, rel = "test-create-relationship")

  private val response: ListLossClaimsResponse[ListLossClaimsItem] = ListLossClaimsResponse(
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

  private val hateoasResponse: ListLossClaimsResponse[HateoasWrapper[ListLossClaimsItem]] = ListLossClaimsResponse(
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

  trait NonTysTest extends Test {
    override def taxYear: String       = "2018-19"
    val rawData: ListLossClaimsRawData = ListLossClaimsRawData(nino, Some(taxYear), Some(selfEmployment), Some(businessId), Some(claimType))

    val request: ListLossClaimsRequest =
      ListLossClaimsRequest(Nino(nino), Some(TaxYear("2019")), None, Some(businessId), Some(TypeOfClaim.`carry-sideways`))
  }

  trait TysTest extends Test {
    override def taxYear: String       = "2023-2024"
    val rawData: ListLossClaimsRawData = ListLossClaimsRawData(nino, Some(taxYear), Some(selfEmployment), Some(businessId), Some(claimType))

    val request: ListLossClaimsRequest =
      ListLossClaimsRequest(Nino(nino), Some(TaxYear("2024")), None, Some(businessId), Some(TypeOfClaim.`carry-sideways`))
  }

  "list" should {
    "return OK" when {
      "the request is valid" in new NonTysTest with Test {
        MockListLossClaimsRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListLossClaimsService
          .list(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrapList(response, ListLossClaimsHateoasData(nino))
          .returns(HateoasWrapper(hateoasResponse, Seq(testCreateHateoasLink)))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseJson))
      }

      "the request is valid for a TYS tax year" in new TysTest with Test {
        MockListLossClaimsRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListLossClaimsService
          .list(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, response))))

        MockHateoasFactory
          .wrapList(response, ListLossClaimsHateoasData(nino))
          .returns(HateoasWrapper(hateoasResponse, Seq(testCreateHateoasLink)))

        runOkTest(expectedStatus = OK, maybeExpectedResponseBody = Some(mtdResponseJson))
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new NonTysTest with Test {
        MockListLossClaimsRequestDataParser
          .parseRequest(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

        runErrorTest(NinoFormatError)
      }

      "the parser validation fails for a TYS tax year" in new TysTest with Test {
        MockListLossClaimsRequestDataParser
          .parseRequest(rawData)
          .returns(Left(ErrorWrapper(correlationId, TaxYearFormatError, None)))

        runErrorTest(TaxYearFormatError)
      }

      "the service returns an error" in new NonTysTest with Test {
        MockListLossClaimsRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListLossClaimsService
          .list(request)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, TypeOfClaimFormatError, None))))

        runErrorTest(TypeOfClaimFormatError)
      }

      "the service returns an error for a TYS tax year" in new TysTest with Test {
        MockListLossClaimsRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListLossClaimsService
          .list(request)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, TypeOfLossFormatError, None))))

        runErrorTest(TypeOfLossFormatError)
      }

      "the request received is valid but an empty list of claims is returned from downstream" in new NonTysTest with Test {
        MockListLossClaimsRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListLossClaimsService
          .list(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ListLossClaimsResponse(Nil)))))

        MockHateoasFactory
          .wrapList(ListLossClaimsResponse(List.empty[ListLossClaimsItem]), ListLossClaimsHateoasData(nino))
          .returns(HateoasWrapper(ListLossClaimsResponse(List.empty[HateoasWrapper[ListLossClaimsItem]]), Seq(testCreateHateoasLink)))

        runErrorTest(NotFoundError)
      }

      "the request received is valid but an empty list of claims is returned from downstream for a TYS tax year" in new TysTest with Test {
        MockListLossClaimsRequestDataParser
          .parseRequest(rawData)
          .returns(Right(request))

        MockListLossClaimsService
          .list(request)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ListLossClaimsResponse(Nil)))))

        MockHateoasFactory
          .wrapList(ListLossClaimsResponse(List.empty[ListLossClaimsItem]), ListLossClaimsHateoasData(nino))
          .returns(HateoasWrapper(ListLossClaimsResponse(List.empty[HateoasWrapper[ListLossClaimsItem]]), Seq(testCreateHateoasLink)))

        runErrorTest(NotFoundError)
      }
    }
  }

  trait Test extends ControllerTest {

    def taxYear: String
    private val controller = new ListLossClaimsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      listLossClaimsService = mockListLossClaimsService,
      listLossClaimsParser = mockListLossClaimsRequestDataParser,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] =
      controller.list(nino, Some(taxYear), Some(selfEmployment), Some(businessId), Some(claimType))(fakeRequest)
  }
}
