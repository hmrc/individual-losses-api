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

package api.endpoints.bfLoss.list.v3

import api.models.errors._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.test.Helpers.AUTHORIZATION
import support.V3IntegrationBaseSpec
import support.stubs.{ AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub }

class ListBFLossesControllerISpec extends V3IntegrationBaseSpec {

  private trait Test {

    val nino: String               = "AA123456A"
    val typeOfLoss: Option[String] = None
    val businessId: Option[String] = None

    def taxYearBroughtForwardFrom: Option[String] = None

    def mtdUri: String = s"/$nino/brought-forward-losses"

    def mtdQueryParams: Seq[(String, String)] =
      Seq("taxYearBroughtForwardFrom" -> taxYearBroughtForwardFrom, "typeOfLoss" -> typeOfLoss, "businessId" -> businessId)
        .collect { case (k, Some(v)) =>
          (k, v)
        }

    def downstreamUrl: String

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
      buildRequest(mtdUri)
        .addQueryStringParameters(mtdQueryParams: _*)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.3.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

    val downstreamResponseJson: JsValue =
      Json.parse(s"""
                    |[
                    |  {
                    |    "incomeSourceId": "XAIS12345678911",
                    |    "lossType": "INCOME",
                    |    "broughtForwardLossAmount": 345.67,
                    |    "taxYear": "2019",
                    |    "lossId": "AAZZ1234567890A",
                    |    "submissionDate": "2020-07-13T12:13:48.763Z"
                    |  },
                    |  {
                    |    "incomeSourceId": "XAIS12345678912",
                    |    "incomeSourceType": "04",
                    |    "broughtForwardLossAmount": 385.67,
                    |    "taxYear": "2020",
                    |    "lossId": "AAZZ1234567890B",
                    |    "submissionDate": "2020-08-13T12:13:48.763Z"
                    |  }
                    |]
     """.stripMargin)

    val responseJson: JsValue =
      Json.parse(s"""
                    |{
                    |  "losses": [
                    |    {
                    |      "lossId": "AAZZ1234567890A",
                    |      "businessId": "XAIS12345678911",
                    |      "typeOfLoss": "self-employment",
                    |      "lossAmount": 345.67,
                    |      "taxYearBroughtForwardFrom": "2018-19",
                    |      "lastModified": "2020-07-13T12:13:48.763Z",
                    |      "links": [
                    |        {
                    |          "href": "/individuals/losses/$nino/brought-forward-losses/AAZZ1234567890A",
                    |          "rel": "self",
                    |          "method": "GET"
                    |        }
                    |      ]
                    |    },
                    |    {
                    |      "lossId": "AAZZ1234567890B",
                    |      "businessId": "XAIS12345678912",
                    |      "typeOfLoss": "uk-property-fhl",
                    |      "lossAmount": 385.67,
                    |      "taxYearBroughtForwardFrom": "2019-20",
                    |      "lastModified": "2020-08-13T12:13:48.763Z",
                    |      "links": [
                    |        {
                    |          "href": "/individuals/losses/$nino/brought-forward-losses/AAZZ1234567890B",
                    |          "rel": "self",
                    |          "method": "GET"
                    |        }
                    |      ]
                    |    }
                    |  ],
                    |  "links": [
                    |    {
                    |      "href": "/individuals/losses/$nino/brought-forward-losses",
                    |      "rel": "self",
                    |      "method": "GET"
                    |    },
                    |    {
                    |      "href": "/individuals/losses/$nino/brought-forward-losses",
                    |      "rel": "create-brought-forward-loss",
                    |      "method": "POST"
                    |    }
                    |  ]
                    |}
     """.stripMargin)

  }

  private trait NonTysTest extends Test {
    def downstreamUrl: String = s"/income-tax/brought-forward-losses/$nino"
  }

  private trait TysIfsTest extends Test {
    override def taxYearBroughtForwardFrom: Option[String] = Some("2023-24")

    def downstreamUrl: String = s"/income-tax/brought-forward-losses/23-24/$nino"
  }

  "Calling the ListBFLosses endpoint" should {

    "return a 200 status code" when {

      "query for all losses" in new NonTysTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl, Map.empty, OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying with specific typeOfLoss" in new NonTysTest {
        override val typeOfLoss: Option[String] = Some("uk-property-fhl")
        override val businessId: Option[String] = None

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl, Map("incomeSourceType" -> "04"), OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying with businessId, taxYear and typeOfLoss" in new NonTysTest {
        override def taxYearBroughtForwardFrom: Option[String] = Some("2019-20")
        override val typeOfLoss: Option[String]                = Some("uk-property-fhl")
        override val businessId: Option[String]                = Some("XKIS00000000988")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(
            DownstreamStub.GET,
            downstreamUrl,
            Map("incomeSourceId" -> "XKIS00000000988", "taxYear" -> "2020", "incomeSourceType" -> "04"),
            OK,
            downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying with businessId, typeOfLoss and a Tax Year Specific (TYS) taxYear" in new TysIfsTest {
        override val typeOfLoss: Option[String] = Some("uk-property-fhl")
        override val businessId: Option[String] = Some("XKIS00000000988")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(
            DownstreamStub.GET,
            downstreamUrl,
            Map("incomeSourceId" -> "XKIS00000000988", "incomeSourceType" -> "04"),
            OK,
            downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "handle errors according to spec" when {
      def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $downstreamCode error" in new NonTysTest {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl, Map.empty, downstreamStatus, errorBody(downstreamCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError)
      serviceErrorTest(BAD_REQUEST, "INVALID_TAXYEAR", BAD_REQUEST, TaxYearFormatError)
      serviceErrorTest(BAD_REQUEST, "INVALID_INCOMESOURCEID", BAD_REQUEST, BusinessIdFormatError)
      serviceErrorTest(BAD_REQUEST, "INVALID_INCOMESOURCETYPE", BAD_REQUEST, TypeOfLossFormatError)
      serviceErrorTest(NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError)
      serviceErrorTest(BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)

      // TYS Errors
      serviceErrorTest(BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError)
      serviceErrorTest(BAD_REQUEST, "INVALID_INCOMESOURCE_ID", BAD_REQUEST, BusinessIdFormatError)
      serviceErrorTest(BAD_REQUEST, "INVALID_INCOMESOURCE_TYPE", BAD_REQUEST, TypeOfLossFormatError)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: Option[String],
                              requestTypeOfLoss: Option[String],
                              requestBusinessId: Option[String],
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new NonTysTest {

          override val nino: String                              = requestNino
          override val taxYearBroughtForwardFrom: Option[String] = requestTaxYear
          override val typeOfLoss: Option[String]                = requestTypeOfLoss
          override val businessId: Option[String]                = requestBusinessId

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

      validationErrorTest("BADNINO", None, None, None, BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", Some("XXXX-YY"), None, None, BAD_REQUEST, TaxYearFormatError)
      validationErrorTest("AA123456A", Some("2017-18"), None, None, BAD_REQUEST, RuleTaxYearNotSupportedError)
      validationErrorTest("AA123456A", Some("2019-21"), None, None, BAD_REQUEST, RuleTaxYearRangeInvalidError)
      validationErrorTest("AA123456A", None, Some("bad-loss-type"), None, BAD_REQUEST, TypeOfLossFormatError)
      validationErrorTest("AA123456A", None, Some("self-employment"), Some("bad-self-employment-id"), BAD_REQUEST, BusinessIdFormatError)
    }

  }

}
