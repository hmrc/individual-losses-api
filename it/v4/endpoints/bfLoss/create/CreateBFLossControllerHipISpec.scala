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

package v4.endpoints.bfLoss.create

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.{RuleDuplicateSubmissionError, TypeOfLossFormatError}
import play.api.libs.json._
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers._
import shared.models.errors._
import shared.models.utils.JsonErrorValidators
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class CreateBFLossControllerHipISpec extends IntegrationBaseSpec with JsonErrorValidators {

  val lossId = "AAZZ1234567890a"

  val requestBody: JsValue = Json.parse(
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
    val nino                  = "AA123456A"
    def downstreamUrl: String = s"/itsd/income-sources/brought-forward-losses/$nino"

    def setupStubs(): StubMapping

    def request: WSRequest = {
      setupStubs()
      buildRequest(s"/$nino/brought-forward-losses")
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.4.0+json"),
          (AUTHORIZATION, "Bearer 123"),
          ("suspend-temporal-validations", "true")
        )
    }

    lazy val responseBody: JsValue = Json.parse(
      s"""
        |{
        |  "lossId": "AAZZ1234567890a",
        |  "links": [
        |    {
        |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
        |      "method": "GET",
        |      "rel": "self"
        |    },
        |    {
        |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
        |      "method": "DELETE",
        |      "rel": "delete-brought-forward-loss"
        |    },
        |    {
        |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId/change-loss-amount",
        |      "method": "POST",
        |      "rel": "amend-brought-forward-loss"
        |    }
        |  ]
        |}
      """.stripMargin
    )

    val downstreamResponse: JsValue = Json.parse(
      s"""
        |{
        |  "lossId": "$lossId"
        |}
      """.stripMargin
    )

    def errorBody(code: String): String =
      s"""
        |{
        |  "code": "$code",
        |  "reason": "downstream message"
        |}
      """.stripMargin

  }

  "Calling the create BFLoss Hip endpoint" should {
    "return a 201 status code" when {
      "any valid request is made" in new Test {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.POST, downstreamUrl, OK, downstreamResponse)
        }

        val response: WSResponse = await(request.post(requestBody))
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
          requestBody.update("/taxYearBroughtForwardFrom", JsString("2090-91")),
          RuleTaxYearNotEndedError.withPath("/taxYearBroughtForwardFrom"))
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error" in new Test {

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

        serviceErrorTest(BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError)
        serviceErrorTest(BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError)
        serviceErrorTest(BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError)
        serviceErrorTest(FORBIDDEN, "TAX_YEAR_NOT_ENDED", BAD_REQUEST, RuleTaxYearNotEndedError)
        serviceErrorTest(FORBIDDEN, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
        serviceErrorTest(CONFLICT, "DUPLICATE_SUBMISSION", BAD_REQUEST, RuleDuplicateSubmissionError)
        serviceErrorTest(NOT_FOUND, "INCOME_SOURCE_NOT_FOUND", NOT_FOUND, NotFoundError)
        serviceErrorTest(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
        serviceErrorTest(INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
      }
    }
  }

}
