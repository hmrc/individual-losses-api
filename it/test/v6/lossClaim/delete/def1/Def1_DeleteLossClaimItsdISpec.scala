/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaim.delete.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.{ClaimIdFormatError, RuleOutsideAmendmentWindow, TaxYearClaimedForFormatError}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.{AUTHORIZATION, *}
import shared.models.errors.*
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_DeleteLossClaimItsdISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino              = "AA123456A"
    val claimId           = "AAZZ1234567890a"
    val taxYearClaimedFor = "2019-20"

    private val downstreamTaxYear = "19-20"

    def uri: String    = s"/$nino/loss-claims/$claimId/tax-year/$taxYearClaimedFor"
    def hipUrl: String = s"/itsd/income-sources/claims-for-relief/$nino/$claimId"

    def errorBody(code: String): String =
      s"""
         |[
         |  {
         |    "errorCode": "$code",
         |    "errorDescription": "error message"
         |  }
         |]
         |""".stripMargin

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .addQueryStringParameters("taxYear" -> downstreamTaxYear)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.6.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

  "Calling the delete Loss Claim endpoint" should {

    "return a 204 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.DELETE, hipUrl, NO_CONTENT, JsObject.empty)
        }

        val response: WSResponse = await(request().delete())
        response.status shouldBe NO_CONTENT
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "handle errors according to spec" when {
      def serviceErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $desCode error" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.DELETE, hipUrl, desStatus, errorBody(desCode))
          }

          val response: WSResponse = await(request().delete())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError)
      serviceErrorTest(BAD_REQUEST, "1220", BAD_REQUEST, ClaimIdFormatError)
      serviceErrorTest(NOT_IMPLEMENTED, "5000", BAD_REQUEST, RuleTaxYearNotSupportedError)
      serviceErrorTest(BAD_REQUEST, "UNEXPECTED_DOWNSTREAM_ERROR_CODE", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(NOT_FOUND, "5010", NOT_FOUND, NotFoundError)
      serviceErrorTest(BAD_REQUEST, "1117", BAD_REQUEST, TaxYearClaimedForFormatError)
      serviceErrorTest(INTERNAL_SERVER_ERROR, "1216", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "4200", BAD_REQUEST, RuleOutsideAmendmentWindow)
      serviceErrorTest(INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestClaimId: String,
                              requestTaxYearClaimedFor: String,
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String              = requestNino
          override val claimId: String           = requestClaimId
          override val taxYearClaimedFor: String = requestTaxYearClaimedFor
          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(requestNino)
          }

          val response: WSResponse = await(request().delete())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      validationErrorTest("BADNINO", "AAZZ1234567890a", "2019-20", BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", "BADCLAIMID", "2019-20", BAD_REQUEST, ClaimIdFormatError)
      validationErrorTest("AA123456A", "AAZZ1234567890a", "BADTAXYEAR", BAD_REQUEST, TaxYearClaimedForFormatError)
      validationErrorTest("AA123456A", "AAZZ1234567890a", "2020-22", BAD_REQUEST, RuleTaxYearRangeInvalidError)
      validationErrorTest("AA123456A", "AAZZ1234567890a", "2017-18", BAD_REQUEST, RuleTaxYearNotSupportedError)
    }

  }

}
