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

package v6.bfLosses.create.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.{RuleBflNotSupportedForFhlProperties, RuleDuplicateSubmissionError, RuleOutsideAmendmentWindow, TypeOfLossFormatError}
import play.api.libs.json.*
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*
import shared.models.domain.TaxYear
import shared.models.domain.TaxYear.currentTaxYear
import shared.models.errors.*
import shared.models.utils.JsonErrorValidators
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_CreateBFLossControllerHipISpec extends IntegrationBaseSpec with JsonErrorValidators {

  private val lossId: String = "AAZZ1234567890a"

  private val requestBody: JsValue = Json.parse(
    """
      |{
      |  "businessId": "XKIS00000000988",
      |  "typeOfLoss": "self-employment",
      |  "taxYearBroughtForwardFrom": "2018-19",
      |  "lossAmount": 256.78
      |}
    """.stripMargin
  )

  private trait Test {
    val nino: String             = "AA123456A"
    private val taxYear: TaxYear = TaxYear.fromMtd("2023-24")

    def downstreamUrl: String = s"/itsd/income-sources/brought-forward-losses/$nino"

    val suspendTemporalValidations: String = "false"

    def setupStubs(): StubMapping

    def request: WSRequest = {
      setupStubs()
      buildRequest(s"/$nino/brought-forward-losses/tax-year/brought-forward-from/${taxYear.asMtd}")
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.6.0+json"),
          (AUTHORIZATION, "Bearer 123"),
          ("suspend-temporal-validations", suspendTemporalValidations)
        )
    }

    lazy val responseBody: JsValue = Json.parse(
      s"""
        |{
        |  "lossId": "$lossId"
        |}
      """.stripMargin
    )

    def errorBody(code: String): String =
      s"""
        |[
        |  {
        |    "errorCode": "$code",
        |    "errorDescription": "downstream message"
        |  }
        |]
      """.stripMargin

  }

  "Calling the create BFLoss endpoint" should {
    "return a 201 status code" when {
      "a valid request is made with a past tax year in the body and suspendTemporalValidations is false" in new Test {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.POST, downstreamUrl, OK, responseBody)
        }

        val response: WSResponse = await(request.post(requestBody))
        response.json shouldBe responseBody
        response.status shouldBe CREATED
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }

      "a valid request is made with the current tax year in the body and suspendTemporalValidations is true" in new Test {
        override val suspendTemporalValidations: String = "true"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.POST, downstreamUrl, OK, responseBody)
        }

        val response: WSResponse = await(request.post(requestBody.update("/taxYearBroughtForwardFrom", JsString(currentTaxYear.asMtd))))
        response.json shouldBe responseBody
        response.status shouldBe CREATED
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {
      "validation error" when {
        def validationErrorTest(requestNino: String, requestBody: JsValue, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {

            override val nino: String = requestNino
            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(requestNino)
            }

            val response: WSResponse = await(request.post(requestBody))
            response.json shouldBe Json.toJson(expectedBody)
            response.status shouldBe BAD_REQUEST
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        validationErrorTest("BADNINO", requestBody, NinoFormatError)
        validationErrorTest(
          "AA123456A",
          requestBody.update("/taxYearBroughtForwardFrom", JsString("XXX")),
          TaxYearFormatError.withPath("/taxYearBroughtForwardFrom")
        )
        validationErrorTest(
          "AA123456A",
          requestBody.update("/taxYearBroughtForwardFrom", JsString("2021-23")),
          RuleTaxYearRangeInvalidError.withPath("/taxYearBroughtForwardFrom")
        )
        validationErrorTest(
          "AA123456A",
          requestBody.update("/taxYearBroughtForwardFrom", JsString("2017-18")),
          RuleTaxYearNotSupportedError.withPath("/taxYearBroughtForwardFrom")
        )
        validationErrorTest("AA123456A", requestBody.update("/lossAmount", JsNumber(12.345)), ValueFormatError.withPath("/lossAmount"))
        validationErrorTest("AA123456A", requestBody.update("/businessId", JsString("not-a-business-id")), BusinessIdFormatError)
        validationErrorTest("AA123456A", requestBody.removeProperty("/lossAmount"), RuleIncorrectOrEmptyBodyError.withPath("/lossAmount"))
        validationErrorTest(
          "AA123456A",
          requestBody.update("/typeOfLoss", JsString("not-a-loss-type")),
          TypeOfLossFormatError.withPath("/typeOfLoss"))
        validationErrorTest(
          "AA123456A",
          requestBody.update("/taxYearBroughtForwardFrom", JsString(currentTaxYear.asMtd)),
          RuleTaxYearNotEndedError.withPath("/taxYearBroughtForwardFrom"))
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns a code $downstreamCode error and status $downstreamStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.POST, downstreamUrl, downstreamStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request.post(requestBody))
            response.json shouldBe Json.toJson(expectedBody)
            response.status shouldBe expectedStatus
            response.header("X-CorrelationId").nonEmpty shouldBe true
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        serviceErrorTest(BAD_REQUEST, "1000", INTERNAL_SERVER_ERROR, InternalError)
        serviceErrorTest(NOT_FOUND, "1002", NOT_FOUND, NotFoundError)
        serviceErrorTest(FORBIDDEN, "1103", BAD_REQUEST, RuleTaxYearNotEndedError)
        serviceErrorTest(BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError)
        serviceErrorTest(UNPROCESSABLE_ENTITY, "1126", BAD_REQUEST, RuleBflNotSupportedForFhlProperties)
        serviceErrorTest(BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError)
        serviceErrorTest(BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, InternalError)
        serviceErrorTest(CONFLICT, "1226", BAD_REQUEST, RuleDuplicateSubmissionError)
        serviceErrorTest(UNPROCESSABLE_ENTITY, "4200", BAD_REQUEST, RuleOutsideAmendmentWindow)
        serviceErrorTest(FORBIDDEN, "5000", BAD_REQUEST, RuleTaxYearNotSupportedError)

      }
    }
  }

}
