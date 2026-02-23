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
import common.errors.RuleOutsideAmendmentWindow
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*
import shared.models.domain.TaxYear
import shared.models.errors.*
import shared.models.utils.JsonErrorValidators
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec
import v7.lossesAndClaims.createAmend.fixtures.CreateAmendLossesAndClaimsFixtures.requestBodyJson

import scala.math.Ordering.Implicits.infixOrderingOps

class CreateAmendLossesAndClaimsControllerISpec extends IntegrationBaseSpec with JsonErrorValidators {

  private val parsedTaxYear: TaxYear   = TaxYear.fromMtd("2026-27")
  private val notEndedTaxYear: TaxYear = parsedTaxYear.max(TaxYear.currentTaxYear)

  private val invalidFieldsRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "claims": {
      |    "carryBack": {
      |      "previousYearGeneralIncome": -5000.99,
      |      "earlyYearLosses": -5000.99,
      |      "terminalLosses": -5000.99
      |    },
      |    "carrySideways": {
      |      "currentYearGeneralIncome": -5000.99
      |    },
      |    "preferenceOrder": {
      |      "applyFirst": "carry-bag"
      |    },
      |    "carryForward": {
      |      "currentYearLosses": -5000.99,
      |      "previousYearsLosses": -5000.99
      |    }
      |  },
      |  "losses": {
      |    "broughtForwardLosses": -5000.99
      |  }
      |}
    """.stripMargin
  )

  private val invalidFieldsRequestErrors: List[MtdError] = List(
    PreferenceOrderFormatError.withPath("/claims/preferenceOrder/applyFirst"),
    ValueFormatError.withPaths(
      List(
        "/claims/carryBack/previousYearGeneralIncome",
        "/claims/carryBack/earlyYearLosses",
        "/claims/carryBack/terminalLosses",
        "/claims/carrySideways/currentYearGeneralIncome",
        "/claims/carryForward/currentYearLosses",
        "/claims/carryForward/previousYearsLosses",
        "/losses/broughtForwardLosses"
      )
    ),
    RuleCarryForwardAndTerminalLossNotAllowedError.withPath("/claims")
  )

  private val wrappedErrors: ErrorWrapper = ErrorWrapper(
    correlationId = "ignored",
    error = BadRequestError,
    errors = Some(invalidFieldsRequestErrors)
  )

  private trait Test {

    val nino: String                       = "AA123456A"
    val businessId: String                 = "XAIS12345678910"
    val taxYear: String                    = parsedTaxYear.asMtd
    val suspendTemporalValidations: String = "true"

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

    private def mtdUri: String = s"/$nino/businesses/$businessId/loss-claims/$taxYear"

    def downstreamUrl: String = s"/itsd/reliefs/loss-claims/$nino/$businessId"

    def request(): WSRequest = {
      setupStubs()
      buildRequest(mtdUri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.7.0+json"),
          (AUTHORIZATION, "Bearer 123"),
          ("suspend-temporal-validations", suspendTemporalValidations)
        )
    }

  }

  "Calling the Create or Amend Losses and Claims endpoint" should {
    "return a 204 status code" when {
      "any valid request is made with a supported tax year that has not ended and suspendTemporalValidations is true" in new Test {
        override val taxYear: String = notEndedTaxYear.asMtd

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(
            method = DownstreamStub.PUT,
            uri = downstreamUrl,
            queryParams = Map("taxYear" -> notEndedTaxYear.asTysDownstream),
            status = NO_CONTENT,
            body = JsObject.empty
          )
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe NO_CONTENT
        response.body shouldBe ""
        response.header("Content-Type") shouldBe None
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {
      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestBusinessId: String,
                                requestTaxYear: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedError: MtdError,
                                expectedErrors: Option[ErrorWrapper]): Unit = {
          s"validation fails with ${expectedError.code} error" in new Test {

            override val nino: String                       = requestNino
            override val businessId: String                 = requestBusinessId
            override val taxYear: String                    = requestTaxYear
            override val suspendTemporalValidations: String = (expectedError != RuleTaxYearNotEndedError).toString

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              MtdIdLookupStub.ninoFound(nino)
              AuthStub.authorised()
            }

            val response: WSResponse = await(request().put(requestBody))
            response.status shouldBe expectedStatus
            response.json shouldBe expectedErrors.fold(Json.toJson(expectedError))(errorWrapper => Json.toJson(errorWrapper))
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = List(
          ("AA1123A", "XAIS12345678910", "2026-27", requestBodyJson, BAD_REQUEST, NinoFormatError, None),
          ("AA123456A", "invalid", "2026-27", requestBodyJson, BAD_REQUEST, BusinessIdFormatError, None),
          ("AA123456A", "XAIS12345678910", "invalid", requestBodyJson, BAD_REQUEST, TaxYearFormatError, None),
          ("AA123456A", "XAIS12345678910", "2025-27", requestBodyJson, BAD_REQUEST, RuleTaxYearRangeInvalidError, None),
          ("AA123456A", "XAIS12345678910", "2025-26", requestBodyJson, BAD_REQUEST, RuleTaxYearNotSupportedError, None),
          ("AA123456A", "XAIS12345678910", notEndedTaxYear.asMtd, requestBodyJson, BAD_REQUEST, RuleTaxYearNotEndedError, None),
          ("AA123456A", "XAIS12345678910", "2026-27", JsObject.empty, BAD_REQUEST, RuleIncorrectOrEmptyBodyError, None),
          ("AA123456A", "XAIS12345678910", "2026-27", invalidFieldsRequestBodyJson, BAD_REQUEST, BadRequestError, Some(wrappedErrors)),
          (
            "AA123456A",
            "XAIS12345678910",
            "2026-27",
            requestBodyJson.removeProperty("/claims/preferenceOrder"),
            BAD_REQUEST,
            RuleMissingPreferenceOrderError.withPath("/claims"),
            None),
          (
            "AA123456A",
            "XAIS12345678910",
            "2026-27",
            requestBodyJson.removeProperty("/claims/carrySideways"),
            BAD_REQUEST,
            RulePreferenceOrderNotAllowedError.withPath("/claims/preferenceOrder/applyFirst"),
            None)
        )

        input.foreach(validationErrorTest.tupled)
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns a code $downstreamCode error and status $downstreamStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              MtdIdLookupStub.ninoFound(nino)
              AuthStub.authorised()
              DownstreamStub.onError(
                method = DownstreamStub.PUT,
                uri = downstreamUrl,
                queryParams = Map("taxYear" -> parsedTaxYear.asTysDownstream),
                errorStatus = downstreamStatus,
                errorBody = errorBody(downstreamCode)
              )
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.json shouldBe Json.toJson(expectedBody)
            response.status shouldBe expectedStatus
            response.header("X-CorrelationId").nonEmpty shouldBe true
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val errors = List(
          (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "1000", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "1007", BAD_REQUEST, BusinessIdFormatError),
          (UNPROCESSABLE_ENTITY, "1115", BAD_REQUEST, RuleTaxYearNotEndedError),
          (UNPROCESSABLE_ENTITY, "1253", BAD_REQUEST, RuleMissingPreferenceOrderError),
          (UNPROCESSABLE_ENTITY, "1254", BAD_REQUEST, RuleCarryForwardAndTerminalLossNotAllowedError),
          (UNPROCESSABLE_ENTITY, "1262", BAD_REQUEST, RuleCarryBackClaimError),
          (UNPROCESSABLE_ENTITY, "4200", BAD_REQUEST, RuleOutsideAmendmentWindow),
          (NOT_IMPLEMENTED, "5000", INTERNAL_SERVER_ERROR, InternalError),
          (NOT_FOUND, "5010", NOT_FOUND, NotFoundError),
          (BAD_REQUEST, "UNMATCHED_STUB_ERROR", BAD_REQUEST, RuleIncorrectGovTestScenarioError)
        )

        errors.foreach(serviceErrorTest.tupled)
      }
    }
  }

}
