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

package v7.lossesAndClaims

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.RuleOutsideAmendmentWindow
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors.*
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class CreateAmendLossesAndClaimsControllerISpec extends IntegrationBaseSpec {

  val typeOfClaim = "carry-forward"
  val typeOfLoss  = "self-employment"
  val claimId     = "AAZZ1234567890a"

  private trait Test {

    val nino       = "AA123456A"
    val businessId = "XKIS00000000988"
    val taxYear    = "2026-27"
    val allowTemporalValidationSuspension: String = "true"
    
    val requestJson: JsValue = Json.parse(s"""
         |{
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

    val downstreamRequestJson: JsValue = Json.parse(s"""
         |{
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

    def errorBody(code: String): String =
      s"""
         |[
         |  {
         |    "errorCode": "$code",
         |    "errorDescription": "string"
         |  }
         |]
      """.stripMargin

    def setupStubs(): StubMapping

    def uri: String

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.7.0+json"),
          (AUTHORIZATION, "Bearer 123"),
          ("suspend-temporal-validations", allowTemporalValidationSuspension)
        )
    }

  }

  "Calling the create amend losses and claims endpoint" should {

    trait CreateAmendLossesAnsClaimsControllerTest extends Test {
      def uri: String = s"/$nino/businesses/$businessId/loss-claims/$taxYear"

      def downstreamURL: String = s"/itsd/reliefs/loss-claims/$nino/$businessId"
    }

    "return a 204 status code" when {
      "any valid request is made" in new CreateAmendLossesAnsClaimsControllerTest() {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamURL, NO_CONTENT)
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe NO_CONTENT
      }
    }

    "downstream service error" when {

      createErrorTest(BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError)
      createErrorTest(BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError)
      createErrorTest(BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, InternalError)
      createErrorTest(BAD_REQUEST, "1000", INTERNAL_SERVER_ERROR, InternalError)
      createErrorTest(BAD_REQUEST, "1007", BAD_REQUEST, BusinessIdFormatError)
      createErrorTest(UNPROCESSABLE_ENTITY, "1115", BAD_REQUEST, RuleTaxYearNotEndedError)
      createErrorTest(UNPROCESSABLE_ENTITY, "1253", BAD_REQUEST, RuleMissingPreferenceOrder)
      createErrorTest(UNPROCESSABLE_ENTITY, "1254", BAD_REQUEST, RuleCarryForwardAndTerminalLossNotAllowed)
      createErrorTest(UNPROCESSABLE_ENTITY, "1262", BAD_REQUEST, RuleCarryBackClaim)
      createErrorTest(UNPROCESSABLE_ENTITY, "4200", BAD_REQUEST, RuleOutsideAmendmentWindow)
      createErrorTest(NOT_IMPLEMENTED, "5000", BAD_REQUEST, RuleTaxYearNotSupportedError)
      createErrorTest(NOT_FOUND, "5010", NOT_FOUND, NotFoundError)
      createErrorTest(BAD_REQUEST, "UNMATCHED_STUB_ERROR", BAD_REQUEST, RuleIncorrectGovTestScenarioError)
    }

    def createErrorTest(status: Int, code: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
      s"downstream returns an $code error" in new CreateAmendLossesAnsClaimsControllerTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onError(DownstreamStub.PUT, downstreamURL, status, errorBody(code))
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }
  }

}
