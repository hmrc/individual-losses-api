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

package v3.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.V3IntegrationBaseSpec
import v3.models.errors._
import v3.stubs.{AuditStub, AuthStub, IfsStub, MtdIdLookupStub}

class ListLossClaimsControllerISpec extends V3IntegrationBaseSpec {

  val correlationId = "X-123"

  val lossAmount = 531.99

  val downstreamResponseJson: JsValue =
    Json.parse(s"""[
        |    {
        |        "incomeSourceId": "XAIS12345678910",
        |        "reliefClaimed": "CF",
        |        "taxYearClaimedFor": "2021",
        |        "claimId": "000000000000011",
        |        "submissionDate": "2021-07-13T12:13:48.763Z",
        |        "sequence": 1
        |    },
        |    {
        |        "incomeSourceId": "XAIS12345678911",
        |        "incomeSourceType": "02",
        |        "reliefClaimed": "CSGI",
        |        "taxYearClaimedFor": "2020",
        |        "claimId": "000000000000022",
        |        "submissionDate": "2020-07-13T12:13:48.763Z",
        |        "sequence": 2
        |    }
        |]
        |
     """.stripMargin)

  private trait Test {

    val nino                             = "AA123456A"
    val taxYear: Option[String]          = None
    val typeOfLoss: Option[String]       = None
    val businessId: Option[String]       = None
    val claimType: Option[String]        = None

    def uri: String = s"/$nino/loss-claims"

    val responseJson: JsValue = Json.parse(s"""
                                              |{
                                              |    "claims": [
                                              |        {
                                              |            "businessId": "XAIS12345678910",
                                              |            "typeOfClaim": "carry-forward",
                                              |            "typeOfLoss": "self-employment",
                                              |            "taxYearClaimedFor": "2020-21",
                                              |            "claimId": "000000000000011",
                                              |            "sequence": 1,
                                              |            "lastModified": "2021-07-13T12:13:48.763Z",
                                              |            "links" : [
                                              |             {
                                              |               "href" : "/individuals/losses$uri/000000000000011",
                                              |               "method": "GET",
                                              |               "rel": "self"
                                              |             }
                                              |            ]
                                              |        },
                                              |        {
                                              |            "businessId": "XAIS12345678911",
                                              |            "typeOfClaim": "carry-sideways",
                                              |            "typeOfLoss": "uk-property-non-fhl",
                                              |            "taxYearClaimedFor": "2019-20",
                                              |            "claimId": "000000000000022",
                                              |            "sequence": 2,
                                              |            "lastModified": "2020-07-13T12:13:48.763Z",
                                              |            "links" : [
                                              |             {
                                              |               "href" : "/individuals/losses$uri/000000000000022",
                                              |               "method": "GET",
                                              |               "rel": "self"
                                              |             }
                                              |            ]
                                              |        }
                                              |    ],
                                              |    "links": [
                                              |      {
                                              |        "href": "/individuals/losses$uri",
                                              |        "method": "GET",
                                              |        "rel": "self"
                                              |      },
                                              |      {
                                              |        "href": "/individuals/losses$uri",
                                              |        "method": "POST",
                                              |        "rel": "create-loss-claim"

                                              |      },
                                              |      {
                                              |        "href": "/individuals/losses$uri/order",
                                              |        "method": "PUT",
                                              |        "rel": "amend-loss-claim-order"
                                              |      }
                                              |    ]
                                              |}
     """.stripMargin)
    def queryParams: Seq[(String, String)] =
      Seq("taxYear" -> taxYear, "typeOfLoss" -> typeOfLoss, "businessId" -> businessId, "claimType" -> claimType)
        .collect {
          case (k, Some(v)) => (k, v)
        }

    def ifsUrl: String = s"/income-tax/claims-for-relief/$nino"

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
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.3.0+json"))
    }

  }

  "Calling the ListLossClaims endpoint" should {

    "return a 200 status code" when {

      "query for everything" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          IfsStub.onSuccess(IfsStub.GET, ifsUrl, Map.empty, Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying for property" in new Test {
        override val taxYear: Option[String]          = None
        override val typeOfLoss: Option[String]       = Some("uk-property-non-fhl")
        override val businessId: Option[String] = None

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          IfsStub.onSuccess(IfsStub.GET, ifsUrl, Map("incomeSourceType" -> "02"), Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying for self-employment" in new Test {
        override val taxYear: Option[String]          = None
        override val typeOfLoss: Option[String]       = Some("self-employment")
        override val businessId: Option[String]       = Some("XKIS00000000988")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          IfsStub.onSuccess(IfsStub.GET, ifsUrl, Map("incomeSourceId" -> "XKIS00000000988"), Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying for selfEmploymentId with no typeOfLoss" in new Test {
        override val taxYear: Option[String]          = None
        override val businessId: Option[String] = Some("XKIS00000000988")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          IfsStub.onSuccess(IfsStub.GET, ifsUrl, Map("incomeSourceId" -> "XKIS00000000988"), Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying for self-employment with no selfEmploymentId" in new Test {
        override val taxYear: Option[String]          = None
        override val typeOfLoss: Option[String]       = Some("self-employment")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          IfsStub.onSuccess(IfsStub.GET, ifsUrl, Map("incomeSourceType" -> "01"), Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "query with taxYear" in new Test {
        override val taxYear: Option[String]          = Some("2019-20")
        override val typeOfLoss: Option[String]       = Some("self-employment")
        override val businessId: Option[String] = Some("XKIS00000000988")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          IfsStub.onSuccess(IfsStub.GET, ifsUrl, Map("incomeSourceId" -> "XKIS00000000988", "taxYear" -> "2020"), Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "query with taxYear only" in new Test {
        override val taxYear: Option[String] = Some("2019-20")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          IfsStub.onSuccess(IfsStub.GET, ifsUrl, Map("taxYear" -> "2020"), Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "handle errors according to spec" when {
      def serviceErrorTest(ifsStatus: Int, ifsCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $ifsCode error" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            IfsStub.onError(IfsStub.GET, ifsUrl, Map.empty, ifsStatus, errorBody(ifsCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXYEAR", Status.BAD_REQUEST, TaxYearFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_INCOMESOURCEID", Status.BAD_REQUEST, BusinessIdFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_INCOMESOURCETYPE", Status.BAD_REQUEST, TypeOfLossFormatError)
      serviceErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
      serviceErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      serviceErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_CLAIM_TYPE", Status.BAD_REQUEST, TypeOfClaimFormatError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: Option[String],
                              requestTypeOfLoss: Option[String],
                              requestSelfEmploymentId: Option[String],
                              requestclaimType: Option[String],
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String     = requestNino
          override val taxYear: Option[String] = requestTaxYear
          override val typeOfLoss: Option[String] = requestTypeOfLoss
          override val businessId : Option[String] = requestSelfEmploymentId
          override val claimType: Option[String] = requestclaimType

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

      validationErrorTest("BADNINO", None, None, None, None, Status.BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", Some("XXXX-YY"), None, None, None, Status.BAD_REQUEST, TaxYearFormatError)
      validationErrorTest("AA123456A", Some("2018-19"), None, None, None, Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      validationErrorTest("AA123456A", Some("2019-21"), None, None, None, Status.BAD_REQUEST, RuleTaxYearRangeInvalid)
      validationErrorTest("AA123456A", None, Some("bad-loss-type"), None, None, Status.BAD_REQUEST, TypeOfLossFormatError)
      validationErrorTest("AA123456A", None, Some("self-employment"), Some("bad-self-employment-id"), None, Status.BAD_REQUEST, BusinessIdFormatError)
      validationErrorTest("AA123456A", None, Some("uk-property-non-fhl"), Some("XA01234556790"), None, Status.BAD_REQUEST, RuleBusinessId)
      validationErrorTest("AA123456A", None, None, None, Some("bad-claim-type"), Status.BAD_REQUEST, ClaimTypeFormatError)
    }

  }
}
