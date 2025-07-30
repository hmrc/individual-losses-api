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

package v4.endpoints.bfLoss.list

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.TypeOfLossFormatError
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.domain.TaxYear
import shared.models.errors.*
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class ListBFLossesControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino                       = "AA123456A"
    val typeOfLoss: Option[String] = None
    val businessId: Option[String] = None

    def taxYear = "2023-24"

    def mtdUri        = s"/$nino/brought-forward-losses/tax-year/$taxYear"
    def downstreamUri = s"/income-tax/brought-forward-losses/${TaxYear.fromMtd(taxYear).asTysDownstream}/$nino"

    def mtdQueryParams: Seq[(String, String)] =
      List(
        "typeOfLoss" -> typeOfLoss,
        "businessId" -> businessId
      )
        .collect { case (k, Some(v)) =>
          (k, v)
        }

    def errorBody(code: String): String =
      s"""
         | {
         |   "code": "$code",
         |   "reason": "downstream message"
         | }
      """.stripMargin

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(mtdUri)
        .addQueryStringParameters(mtdQueryParams: _*)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.4.0+json"),
          (AUTHORIZATION, "Bearer 123")
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

  "Calling the ListBFLosses endpoint" should {

    "return a 200 status code" when {

      "query for all losses" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map.empty, OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying with specific typeOfLoss = uk-property-fhl" in new Test {
        override val typeOfLoss: Option[String] = Some("uk-property-fhl")
        override val businessId: Option[String] = None

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("incomeSourceType" -> "04"), OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying with specific typeOfLoss = self-employment" in new Test {
        override val typeOfLoss: Option[String] = Some("self-employment")
        override val businessId: Option[String] = None

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("incomeSourceType" -> "01"), OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying with businessId and typeOfLoss" in new Test {
        override val typeOfLoss: Option[String] = Some("uk-property-fhl")
        override val businessId: Option[String] = Some("XKIS00000000988")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(
            DownstreamStub.GET,
            downstreamUri,
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
        s"downstream returns an $downstreamCode error" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.GET, downstreamUri, Map.empty, downstreamStatus, errorBody(downstreamCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError)
      serviceErrorTest(NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError)
      serviceErrorTest(BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError)
      serviceErrorTest(BAD_REQUEST, "INVALID_INCOMESOURCE_ID", BAD_REQUEST, BusinessIdFormatError)
      serviceErrorTest(BAD_REQUEST, "INVALID_INCOMESOURCE_TYPE", BAD_REQUEST, TypeOfLossFormatError)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
      serviceErrorTest(BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: String,
                              requestTypeOfLoss: Option[String],
                              requestBusinessId: Option[String],
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String               = requestNino
          override val taxYear: String            = requestTaxYear
          override val typeOfLoss: Option[String] = requestTypeOfLoss
          override val businessId: Option[String] = requestBusinessId

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

      validationErrorTest("BADNINO", "2023-24", None, None, BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", "XXXX-YY", None, None, BAD_REQUEST, TaxYearFormatError)
      validationErrorTest("AA123456A", "2017-18", None, None, BAD_REQUEST, RuleTaxYearNotSupportedError)
      validationErrorTest("AA123456A", "2019-21", None, None, BAD_REQUEST, RuleTaxYearRangeInvalidError)
      validationErrorTest("AA123456A", "2023-24", Some("bad-loss-type"), None, BAD_REQUEST, TypeOfLossFormatError)
      validationErrorTest("AA123456A", "2023-24", Some("self-employment"), Some("bad-self-employment-id"), BAD_REQUEST, BusinessIdFormatError)
    }

  }

}
