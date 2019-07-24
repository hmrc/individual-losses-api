/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIED OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class ListLossClaimsControllerISpec extends IntegrationBaseSpec {

  val correlationId = "X-123"

  val lossAmount = 531.99

  val responseJson: JsValue = Json.parse(s"""
        |{
        |    "claims": [
        |        {
        |            "id": "000000000000011"
        |        },
        |        {
        |            "id": "000000000000022"
        |        }
        |    ]
        |}
     """.stripMargin)

  val desResponseJson: JsValue =
    Json.parse(s"""[
        |    {
        |        "incomeSourceId": "000000000000001",
        |        "reliefClaimed": "CF",
        |        "taxYearClaimedFor": "2099",
        |        "claimId": "000000000000011",
        |        "submissionDate": "2019-07-13T12:13:48.763Z"
        |    },
        |    {
        |        "incomeSourceId": "000000000000002",
        |        "reliefClaimed": "CF",
        |        "taxYearClaimedFor": "2020",
        |        "claimId": "000000000000022",
        |        "submissionDate": "2018-07-13T12:13:48.763Z"
        |    }
        |]
        |
     """.stripMargin)

  private trait Test {

    val nino                             = "AA123456A"
    val taxYear: Option[String]          = None
    val typeOfLoss: Option[String]       = None
    val selfEmploymentId: Option[String] = None

    def uri: String = s"/$nino/loss-claims"

    def queryParams: Seq[(String, String)] =
      Seq("taxYear" -> taxYear, "typeOfLoss" -> typeOfLoss, "selfEmploymentId" -> selfEmploymentId)
        .collect {
          case (k, Some(v)) => (k, v)
        }

    def desUrl: String = s"/income-tax/claims-for-relief/$nino"

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "des message"
         |      }
      """.stripMargin

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .addQueryStringParameters(queryParams: _*)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

  }

  "Calling the ListLossClaims endpoint" should {

    "return a 200 status code" when {

      "query for everything" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUrl, Map.empty, Status.OK, desResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
      }

      "querying for property" in new Test {
        override val taxYear: Option[String]          = None
        override val typeOfLoss: Option[String]       = Some("uk-property-non-fhl")
        override val selfEmploymentId: Option[String] = None

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUrl, Map("incomeSourceType" -> "02"), Status.OK, desResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
      }

      "querying for self-employment" in new Test {
        override val taxYear: Option[String]          = None
        override val typeOfLoss: Option[String]       = Some("self-employment")
        override val selfEmploymentId: Option[String] = Some("XKIS00000000988")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUrl, Map("incomeSourceId" -> "XKIS00000000988"), Status.OK, desResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
      }

      "querying for selfEmploymentId with no typeOfLoss" in new Test {
        override val taxYear: Option[String]          = None
        override val selfEmploymentId: Option[String] = Some("XKIS00000000988")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUrl, Map("incomeSourceId" -> "XKIS00000000988"), Status.OK, desResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
      }

      "querying for self-employment with no selfEmploymentId" in new Test {
        override val taxYear: Option[String]          = None
        override val typeOfLoss: Option[String]       = Some("self-employment")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUrl, Map("incomeSourceType" -> "01"), Status.OK, desResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
      }

      "query with taxYear" in new Test {
        override val taxYear: Option[String]          = Some("2018-19")
        override val typeOfLoss: Option[String]       = Some("self-employment")
        override val selfEmploymentId: Option[String] = Some("XKIS00000000988")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUrl, Map("incomeSourceId" -> "XKIS00000000988", "taxYear" -> "2019"), Status.OK, desResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
      }

      "query with taxYear only" in new Test {
        override val taxYear: Option[String] = Some("2018-19")

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUrl, Map("taxYear" -> "2019"), Status.OK, desResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
      }
    }

    "handle errors according to spec" when {
      def serviceErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"des returns an $desCode error" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DesStub.onError(DesStub.GET, desUrl, Map.empty, desStatus, errorBody(desCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
        }
      }

      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXYEAR", Status.BAD_REQUEST, TaxYearFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_INCOMESOURCEID", Status.BAD_REQUEST, SelfEmploymentIdFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_INCOMESOURCETYPE", Status.BAD_REQUEST, TypeOfLossFormatError)
      serviceErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
      serviceErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      serviceErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: Option[String],
                              requestTypeOfLoss: Option[String],
                              requestSelfEmploymentId: Option[String],
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String     = requestNino
          override val taxYear          = requestTaxYear
          override val typeOfLoss       = requestTypeOfLoss
          override val selfEmploymentId = requestSelfEmploymentId

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(requestNino)
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
        }
      }

      validationErrorTest("BADNINO", None, None, None, Status.BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", Some("XXXX-YY"), None, None, Status.BAD_REQUEST, TaxYearFormatError)
      validationErrorTest("AA123456A", Some("2010-11"), None, None, Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      validationErrorTest("AA123456A", Some("2019-21"), None, None, Status.BAD_REQUEST, RuleTaxYearRangeExceededError)
      validationErrorTest("AA123456A", None, Some("bad-loss-type"), None, Status.BAD_REQUEST, TypeOfLossFormatError)
      validationErrorTest("AA123456A", None, Some("self-employment"), Some("bad-self-employment-id"), Status.BAD_REQUEST, SelfEmploymentIdFormatError)
      validationErrorTest("AA123456A", None, Some("uk-property-non-fhl"), Some("XA01234556790"), Status.BAD_REQUEST, RuleSelfEmploymentId)
    }

  }
}