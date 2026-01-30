/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors.{
  BusinessIdFormatError,
  InternalError,
  MtdError,
  NinoFormatError,
  NotFoundError,
  RuleTaxYearNotSupportedError,
  RuleTaxYearRangeInvalidError,
  TaxYearFormatError
}
import shared.services.{AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class RetrieveLossesAndClaimsControllerISpec extends IntegrationBaseSpec {

  "Calling the Retrieve Losses and Claims endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl, downstreamQueryParams, Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe mtdResponseJson
        response.status shouldBe Status.OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "validation error" when {
      def validationErrorTest(requestNino: String,
                              requestBusinessId: String,
                              requestTaxYear: String,
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String       = requestNino
          override val businessId: String = requestBusinessId
          override val taxYear: String    = requestTaxYear

          override def setupStubs(): StubMapping = {
            MtdIdLookupStub.ninoFound(nino)
            AuthStub.authorised()
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val input = List(
        ("AA1123A", "XAIS12345678910", "2026-27", BAD_REQUEST, NinoFormatError),
        ("AA123456A", "XAIS12345678910", "invalid", BAD_REQUEST, TaxYearFormatError),
        ("AA123456A", "invalid", "2026-27", BAD_REQUEST, BusinessIdFormatError),
        ("AA123456A", "XAIS12345678910", "2025-27", BAD_REQUEST, RuleTaxYearRangeInvalidError),
        ("AA123456A", "XAIS12345678910", "2025-26", BAD_REQUEST, RuleTaxYearNotSupportedError)
      )

      input.foreach(validationErrorTest.tupled)
    }

    "handle errors according to spec" when {
      def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $downstreamCode error" in new Test {

          override def setupStubs(): StubMapping = {
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl, downstreamStatus, errorBody(downstreamCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(Status.BAD_REQUEST, "1215", Status.BAD_REQUEST, NinoFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "1117", Status.BAD_REQUEST, TaxYearFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "1216", Status.INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(Status.BAD_REQUEST, "1007", Status.BAD_REQUEST, BusinessIdFormatError)
      serviceErrorTest(Status.NOT_FOUND, "5010", Status.NOT_FOUND, NotFoundError)
    }
  }

  private trait Test {

    val nino                      = "AA123456A"
    val businessId                = "XKIS00000000988"
    val taxYear                   = "2026-27"
    private val lastModified      = "2026-08-24T14:15:22.544Z"
    private val downstreamTaxYear = "26-27"

    val downstreamResponseJson: JsValue = Json.parse(s"""
         |{
         |  "submittedOn": "$lastModified",
         |  "claims": {
         |    "carryBack": {
         |      "previousYearGeneralIncomeSection64": 5000.99,
         |      "earlyYearLossesSection72": 5000.99
         |    },
         |    "carrySideways": {
         |      "currentYearGeneralIncomeSection64": 5000.99
         |    },
         |    "preferenceOrderSection64": {
         |      "applyFirst": "carry-sideways"
         |    },
         |    "carryForward": {
         |      "currentYearLossesSection83": 5000.99,
         |      "previousYearsLossesSection83": 5000.99
         |    }
         |  },
         |  "losses": {
         |    "broughtForwardLosses": 5000.99
         |  }
         |}
        """.stripMargin)

    val mtdResponseJson: JsValue = Json.parse(s"""
         |{
         |  "submittedOn": "$lastModified",
         |  "claims": {
         |    "carryBack": {
         |      "previousYearGeneralIncome": 5000.99,
         |      "earlyYearLosses": 5000.99
         |    },
         |    "carrySideways": {
         |      "currentYearGeneralIncome": 5000.99
         |    },
         |    "preferenceOrder": {
         |      "applyFirst": "carry-sideways"
         |    },
         |    "carryForward": {
         |      "currentYearLosses": 5000.99,
         |      "previousYearsLosses": 5000.99
         |    }
         |  },
         |  "losses": {
         |    "broughtForwardLosses": 5000.99
         |  }
         |}
        """.stripMargin)

    def downstreamUrl: String = s"/itsd/reliefs/loss-claims/$nino/$businessId"

    val downstreamQueryParams: Map[String, String] = Map("taxYear" -> downstreamTaxYear)

    def errorBody(code: String): String =
      s"""
         |{
         |  "origin": "HIP",
         |  "response":  [
         |    {
         |      "errorCode": "$code",
         |      "errorDescription": "error message"
         |    }
         |  ]
         |}
         |""".stripMargin

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(s"/$nino/businesses/$businessId/loss-claims/$taxYear")
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.7.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

}
