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

import api.models.errors._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.V3IntegrationBaseSpec
import v3.models.errors._
import v3.stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}

class ListBFLossesControllerISpec extends V3IntegrationBaseSpec {

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

  private trait Test {

    val nino: String                              = "AA123456A"
    val taxYearBroughtForwardFrom: Option[String] = None
    val typeOfLoss: Option[String]                = None
    val businessId: Option[String]                = None

    def uri: String = s"/$nino/brought-forward-losses"

    val responseJson: JsValue = Json.parse(s"""{
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

    def queryParams: Seq[(String, String)] =
      Seq("taxYearBroughtForwardFrom" -> taxYearBroughtForwardFrom, "typeOfLoss" -> typeOfLoss, "businessId" -> businessId)
        .collect {
          case (k, Some(v)) => (k, v)
        }

    def ifsUrl: String = s"/income-tax/brought-forward-losses/$nino"

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

  "Calling the ListBFLosses endpoint" should {

    "return a 200 status code" when {

      "query for all losses" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, ifsUrl, Map.empty, Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying with specific typeOfLoss" in new Test {
        override val taxYearBroughtForwardFrom: Option[String] = None
        override val typeOfLoss: Option[String]                = Some("uk-property-fhl")
        override val businessId: Option[String]                = None

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, ifsUrl, Map("incomeSourceType" -> "04"), Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying with businessId, taxYear and typeOfLoss" in new Test {
        override val taxYearBroughtForwardFrom: Option[String] = Some("2019-20")
        override val typeOfLoss: Option[String]                = Some("self-employment")
        override val businessId: Option[String]                = Some("XKIS00000000988")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET,
                                   ifsUrl,
                                   Map("incomeSourceId" -> "XKIS00000000988", "taxYear" -> "2020"),
                                   Status.OK,
                                   downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
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
            DownstreamStub.onError(DownstreamStub.GET, ifsUrl, Map.empty, ifsStatus, errorBody(ifsCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXYEAR", Status.BAD_REQUEST, TaxYearFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_INCOMESOURCEID", Status.BAD_REQUEST, BusinessIdFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_INCOMESOURCETYPE", Status.BAD_REQUEST, TypeOfLossFormatError)
      serviceErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_CORRELATIONID", Status.INTERNAL_SERVER_ERROR, StandardDownstreamError)
      serviceErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, StandardDownstreamError)
      serviceErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, StandardDownstreamError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: Option[String],
                              requestTypeOfLoss: Option[String],
                              requestBusinessId: Option[String],
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

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

      validationErrorTest("BADNINO", None, None, None, Status.BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", Some("XXXX-YY"), None, None, Status.BAD_REQUEST, TaxYearFormatError)
      validationErrorTest("AA123456A", Some("2017-18"), None, None, Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      validationErrorTest("AA123456A", Some("2019-21"), None, None, Status.BAD_REQUEST, RuleTaxYearRangeInvalid)
      validationErrorTest("AA123456A", None, Some("bad-loss-type"), None, Status.BAD_REQUEST, TypeOfLossFormatError)
      validationErrorTest("AA123456A", None, Some("self-employment"), Some("bad-self-employment-id"), Status.BAD_REQUEST, BusinessIdFormatError)
    }

  }
}
