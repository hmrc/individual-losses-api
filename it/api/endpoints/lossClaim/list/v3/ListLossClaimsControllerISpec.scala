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

import api.models.errors._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.test.Helpers.AUTHORIZATION
import support.V3IntegrationBaseSpec
import support.stubs.{ AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub }

class ListLossClaimsControllerISpec extends V3IntegrationBaseSpec {

  val lossAmount = 531.99

  val downstreamResponseJson: JsValue =
    Json.parse(s"""[
        |    {
        |        "incomeSourceId": "XAIS12345678910",
        |        "incomeSourceType": "02",
        |        "reliefClaimed": "CSGI",
        |        "taxYearClaimedFor": "2020",
        |        "claimId": "AAZZ1234567890A",
        |        "sequence": 1,
        |        "submissionDate": "2020-07-13T12:13:763Z"
        |    },
        |    {
        |        "incomeSourceId": "XAIS12345678911",
        |        "reliefClaimed": "CSGI",
        |        "taxYearClaimedFor": "2021",
        |        "claimId": "AAZZ1234567890B",
        |        "sequence": 2,
        |        "submissionDate": "2021-11-10T11:56:728Z"
        |    }
        |]
        |
     """.stripMargin)

  private trait Test {

    def taxYear: Option[String]
    def downstreamUrl: String
    val nino                       = "AA123456A"
    val typeOfLoss: Option[String] = None
    val businessId: Option[String] = None
    val claimType: Option[String]  = None

    def uri: String = s"/$nino/loss-claims"

    val responseJson: JsValue = Json.parse(s"""
                                              |{
                                              |    "claims": [
                                              |        {
                                              |            "businessId": "XAIS12345678910",
                                              |            "typeOfLoss": "uk-property-non-fhl",
                                              |            "typeOfClaim": "carry-sideways",
                                              |            "taxYearClaimedFor": "2019-20",
                                              |            "claimId": "AAZZ1234567890A",
                                              |            "sequence": 1,
                                              |            "lastModified": "2020-07-13T12:13:763Z",
                                              |            "links" : [
                                              |             {
                                              |               "href" : "/individuals/losses/$nino/loss-claims/AAZZ1234567890A",
                                              |               "rel": "self",
                                              |               "method": "GET"
                                              |             }
                                              |            ]
                                              |        },
                                              |        {
                                              |            "businessId": "XAIS12345678911",
                                              |            "typeOfLoss": "self-employment",
                                              |            "typeOfClaim": "carry-sideways",
                                              |            "taxYearClaimedFor": "2020-21",
                                              |            "claimId": "AAZZ1234567890B",
                                              |            "sequence": 2,
                                              |            "lastModified": "2021-11-10T11:56:728Z",
                                              |            "links" : [
                                              |             {
                                              |               "href" : "/individuals/losses/$nino/loss-claims/AAZZ1234567890B",
                                              |               "rel": "self",
                                              |               "method": "GET"
                                              |             }
                                              |            ]
                                              |        }
                                              |    ],
                                              |    "links": [
                                              |      {
                                              |        "href": "/individuals/losses/$nino/loss-claims",
                                              |        "rel": "self",
                                              |        "method": "GET"
                                              |      },
                                              |      {
                                              |        "href": "/individuals/losses/$nino/loss-claims",
                                              |        "rel": "create-loss-claim",
                                              |        "method": "POST"
                                              |      },
                                              |      {
                                              |        "href": "/individuals/losses/$nino/loss-claims/order",
                                              |        "rel": "amend-loss-claim-order",
                                              |        "method": "PUT"
                                              |      }
                                              |    ]
                                              |}
     """.stripMargin)

    def queryParams: Seq[(String, String)]

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "downstream message"
         |      }
      """.stripMargin

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .addQueryStringParameters(queryParams: _*)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.3.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }
  }

  private trait NonTysTest extends Test {
    override def taxYear: Option[String] = Some("2019-20")

    override def downstreamUrl: String = s"/income-tax/claims-for-relief/$nino"

    override def queryParams: Seq[(String, String)] =
      Seq("taxYearClaimedFor" -> taxYear, "typeOfLoss" -> typeOfLoss, "businessId" -> businessId, "typeOfClaim" -> claimType)
        .collect {
          case (k, Some(v)) => (k, v)
        }
  }

  private trait TysIfsTest extends Test {
    override def taxYear: Option[String] = Some("2023-24")

    override def downstreamUrl: String = s"/income-tax/claims-for-relief/23-24/$nino"

    override def queryParams: Seq[(String, String)] =
      Seq("taxYearClaimedFor" -> taxYear, "typeOfLoss" -> typeOfLoss, "businessId" -> businessId, "typeOfClaim" -> claimType)
        .collect {
          case (k, Some(v)) => (k, v)
        }
  }

  "Calling the ListLossClaims endpoint" should {

    "return a 200 status code" when {

      "query for everything" in new NonTysTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl, Map.empty, Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "query for everything for a TYS tax year" in new TysIfsTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl, Map.empty, Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying for a specific typeOfLoss" in new NonTysTest {
        override val typeOfLoss: Option[String] = Some("uk-property-non-fhl")

        val downstreamResponse: JsValue =
          Json.parse(s"""[
                 |    {
                 |        "incomeSourceId": "XAIS12345678910",
                 |        "incomeSourceType": "02",
                 |        "reliefClaimed": "CSGI",
                 |        "taxYearClaimedFor": "2020",
                 |        "claimId": "AAZZ1234567890A",
                 |        "sequence": 1,
                 |        "submissionDate": "2020-07-13T12:13:763Z"
                 |    }
                 |]
               |""".stripMargin)

        override val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [
             |        {
             |            "businessId": "XAIS12345678910",
             |            "typeOfLoss": "uk-property-non-fhl",
             |            "typeOfClaim": "carry-sideways",
             |            "taxYearClaimedFor": "2019-20",
             |            "claimId": "AAZZ1234567890A",
             |            "sequence": 1,
             |            "lastModified": "2020-07-13T12:13:763Z",
             |            "links" : [
             |             {
             |               "href" : "/individuals/losses/$nino/loss-claims/AAZZ1234567890A",
             |               "rel": "self",
             |               "method": "GET"
             |             }
             |            ]
             |        }
             |    ],
             |    "links": [
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims",
             |        "rel": "self",
             |        "method": "GET"
             |      },
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims",
             |        "rel": "create-loss-claim",
             |        "method": "POST"
             |      },
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims/order",
             |        "rel": "amend-loss-claim-order",
             |        "method": "PUT"
             |      }
             |    ]
             |}""".stripMargin)

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl, Map("incomeSourceType" -> "02"), Status.OK, downstreamResponse)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying for a specific typeOfLoss for a TYS tax year" in new TysIfsTest {
        override val typeOfLoss: Option[String] = Some("uk-property-non-fhl")

        val downstreamResponse: JsValue =
          Json.parse(s"""[
               |    {
               |        "incomeSourceId": "XAIS12345678910",
               |        "incomeSourceType": "02",
               |        "reliefClaimed": "CSGI",
               |        "taxYearClaimedFor": "2020",
               |        "claimId": "AAZZ1234567890A",
               |        "sequence": 1,
               |        "submissionDate": "2020-07-13T12:13:763Z"
               |    }
               |]
               |""".stripMargin)

        override val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [
             |        {
             |            "businessId": "XAIS12345678910",
             |            "typeOfLoss": "uk-property-non-fhl",
             |            "typeOfClaim": "carry-sideways",
             |            "taxYearClaimedFor": "2019-20",
             |            "claimId": "AAZZ1234567890A",
             |            "sequence": 1,
             |            "lastModified": "2020-07-13T12:13:763Z",
             |            "links" : [
             |             {
             |               "href" : "/individuals/losses/$nino/loss-claims/AAZZ1234567890A",
             |               "rel": "self",
             |               "method": "GET"
             |             }
             |            ]
             |        }
             |    ],
             |    "links": [
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims",
             |        "rel": "self",
             |        "method": "GET"
             |      },
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims",
             |        "rel": "create-loss-claim",
             |        "method": "POST"
             |      },
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims/order",
             |        "rel": "amend-loss-claim-order",
             |        "method": "PUT"
             |      }
             |    ]
             |}""".stripMargin)

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl, Map("incomeSourceType" -> "02"), Status.OK, downstreamResponse)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying for specific taxYear and businessId" in new NonTysTest {
        override val taxYear: Option[String]    = Some("2019-20")
        override val businessId: Option[String] = Some("XAIS12345678911")

        val downstreamResponse: JsValue =
          Json.parse(s"""
               |[
               |    {
               |        "incomeSourceId": "XAIS12345678911",
               |        "reliefClaimed": "CSGI",
               |        "taxYearClaimedFor": "2020",
               |        "claimId": "AAZZ1234567890B",
               |        "sequence": 1,
               |        "submissionDate": "2020-07-13T12:13:763Z"
               |    }
               |]
     """.stripMargin)

        override val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [
             |        {
             |            "businessId": "XAIS12345678911",
             |            "typeOfLoss": "self-employment",
             |            "typeOfClaim": "carry-sideways",
             |            "taxYearClaimedFor": "2019-20",
             |            "claimId": "AAZZ1234567890B",
             |            "sequence": 1,
             |            "lastModified": "2020-07-13T12:13:763Z",
             |            "links" : [
             |             {
             |               "href" : "/individuals/losses/$nino/loss-claims/AAZZ1234567890B",
             |               "rel": "self",
             |               "method": "GET"
             |             }
             |            ]
             |        }
             |    ],
             |    "links": [
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims",
             |        "rel": "self",
             |        "method": "GET"
             |      },
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims",
             |        "rel": "create-loss-claim",
             |        "method": "POST"
             |      },
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims/order",
             |        "rel": "amend-loss-claim-order",
             |        "method": "PUT"
             |      }
             |    ]
             |}
     """.stripMargin)

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET,
                                   downstreamUrl,
                                   queryParams = Map("incomeSourceId" -> "XAIS12345678911", "taxYear" -> "2020"),
                                   Status.OK,
                                   downstreamResponse)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "querying for a TYS specific taxYear and businessId" in new NonTysTest {
      override val businessId: Option[String] = Some("XAIS12345678911")

      val downstreamResponse: JsValue =
        Json.parse(s"""
               |[
               |    {
               |        "incomeSourceId": "XAIS12345678911",
               |        "reliefClaimed": "CSGI",
               |        "taxYearClaimedFor": "2020",
               |        "claimId": "AAZZ1234567890B",
               |        "sequence": 1,
               |        "submissionDate": "2020-07-13T12:13:763Z"
               |    }
               |]
     """.stripMargin)

      override val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [
             |        {
             |            "businessId": "XAIS12345678911",
             |            "typeOfLoss": "self-employment",
             |            "typeOfClaim": "carry-sideways",
             |            "taxYearClaimedFor": "2019-20",
             |            "claimId": "AAZZ1234567890B",
             |            "sequence": 1,
             |            "lastModified": "2020-07-13T12:13:763Z",
             |            "links" : [
             |             {
             |               "href" : "/individuals/losses/$nino/loss-claims/AAZZ1234567890B",
             |               "rel": "self",
             |               "method": "GET"
             |             }
             |            ]
             |        }
             |    ],
             |    "links": [
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims",
             |        "rel": "self",
             |        "method": "GET"
             |      },
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims",
             |        "rel": "create-loss-claim",
             |        "method": "POST"
             |      },
             |      {
             |        "href": "/individuals/losses/$nino/loss-claims/order",
             |        "rel": "amend-loss-claim-order",
             |        "method": "PUT"
             |      }
             |    ]
             |}
     """.stripMargin)

      override def setupStubs(): StubMapping = {
        AuditStub.audit()
        AuthStub.authorised()
        MtdIdLookupStub.ninoFound(nino)
        DownstreamStub.onSuccess(DownstreamStub.GET,
                                 downstreamUrl,
                                 queryParams = Map("incomeSourceId" -> "XAIS12345678911"),
                                 Status.OK,
                                 downstreamResponse)
      }

      val response: WSResponse = await(request().get())
      response.json shouldBe responseJson
      response.status shouldBe Status.OK
      response.header("Content-Type") shouldBe Some("application/json")
    }

    "return a 404 status code" when {
      "an empty array (no loss claims exists) is returned from backend" in new NonTysTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(
            DownstreamStub.GET,
            downstreamUrl,
            Map.empty,
            Status.OK,
            Json.parse("""
                |[
                |
                |]
                |""".stripMargin)
          )
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe Json.toJson(NotFoundError)
        response.status shouldBe Status.NOT_FOUND
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return a 500 status code" when {
      "empty loss claims object inside the array is returned from backend" in new NonTysTest {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(
            DownstreamStub.GET,
            downstreamUrl,
            Map.empty,
            Status.OK,
            Json.parse("""
                |[
                |{}
                |]
                |""".stripMargin)
          )
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.INTERNAL_SERVER_ERROR
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "handle errors according to spec" when {
      def serviceErrorTest(ifsStatus: Int, ifsCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $ifsCode error" in new NonTysTest {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl, Map.empty, ifsStatus, errorBody(ifsCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val errors = Seq(
        (Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError),
        (Status.BAD_REQUEST, "INVALID_TAXYEAR", Status.BAD_REQUEST, TaxYearFormatError),
        (Status.BAD_REQUEST, "INVALID_INCOMESOURCEID", Status.BAD_REQUEST, BusinessIdFormatError),
        (Status.BAD_REQUEST, "INVALID_INCOMESOURCETYPE", Status.BAD_REQUEST, TypeOfLossFormatError),
        (Status.BAD_REQUEST, "INVALID_CORRELATIONID", Status.INTERNAL_SERVER_ERROR, InternalError),
        (Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError),
        (Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, InternalError),
        (Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, InternalError),
        (Status.BAD_REQUEST, "INVALID_CLAIM_TYPE", Status.BAD_REQUEST, TypeOfClaimFormatError)
      )

      val tysErrors = Seq(
        (Status.BAD_REQUEST, "INVALID_CORRELATION_ID", Status.INTERNAL_SERVER_ERROR, InternalError),
        (Status.BAD_REQUEST, "INVALID_TAX_YEAR", Status.BAD_REQUEST, TaxYearFormatError),
        (Status.BAD_REQUEST, "INVALID_INCOMESOURCE_ID", Status.BAD_REQUEST, BusinessIdFormatError),
        (Status.BAD_REQUEST, "INVALID_INCOMESOURCE_TYPE", Status.BAD_REQUEST, TypeOfLossFormatError),
        (Status.BAD_REQUEST, "TAX_YEAR_NOT_SUPPORTED", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      )

      (errors ++ tysErrors).foreach(_ => (serviceErrorTest _).tupled)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: Option[String],
                              requestTypeOfLoss: Option[String],
                              requestSelfEmploymentId: Option[String],
                              requestClaimType: Option[String],
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new NonTysTest {

          override val nino: String               = requestNino
          override val taxYear: Option[String]    = requestTaxYear
          override val typeOfLoss: Option[String] = requestTypeOfLoss
          override val businessId: Option[String] = requestSelfEmploymentId
          override val claimType: Option[String]  = requestClaimType

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(requestNino)
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val errors = Seq(
        ("AA1234", None, None, None, None, Status.BAD_REQUEST, NinoFormatError),
        ("AA123456A", Some("XXXX-YY"), None, None, None, Status.BAD_REQUEST, TaxYearFormatError),
        ("AA123456A", Some("2018-19"), None, None, None, Status.BAD_REQUEST, RuleTaxYearNotSupportedError),
        ("AA123456A", Some("2019-21"), None, None, None, Status.BAD_REQUEST, RuleTaxYearRangeInvalidError),
        ("AA123456A", None, Some("employment"), None, None, Status.BAD_REQUEST, TypeOfLossFormatError),
        ("AA123456A", None, Some("self-employment"), Some("XKIS0000000"), None, Status.BAD_REQUEST, BusinessIdFormatError),
        ("AA123456A", None, None, None, Some("FORWARD"), Status.BAD_REQUEST, TypeOfClaimFormatError)
      )

      errors.foreach(_ => (validationErrorTest _).tupled)
    }

  }
}
