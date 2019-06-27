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
import v1.models.errors.{DownstreamError, NinoFormatError, NotFoundError, RuleTaxYearRangeExceededError, RuleTypeOfLossUnsupported}
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class CreateBFLossControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino = "AA123456A"
    val lossId = "AAZZ1234567890a"
    val correlationId = "X-123"

    val requestBody: JsValue = Json.parse(
      """
        |{
        |    "selfEmploymentId": "XKIS00000000988",
        |    "typeOfLoss": "self-employment",
        |    "taxYear": "2019-20",
        |    "lossAmount": 256.78
        |}
      """.stripMargin)

    val responseBody: JsValue = Json.parse(
      """
        |{
        |    "id": "dfgdfgdfg"
        |}
      """.stripMargin)

    def setupStubs(): StubMapping

    def uri: String

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "des message"
         |      }
      """.stripMargin
  }

  "Calling the create BFLoss endpoint" should {

    trait CreateBFLossControllerTest extends Test {
      def uri: String = s"/individual/losses/$nino/brought-forward-losses"
    }

    "return a 201 status code" when {

      "any valid request is made" in new CreateBFLossControllerTest() {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.serviceSuccess(nino)
        }

        val response: WSResponse = await(request().post(requestBody))
        response.status shouldBe Status.CREATED
        response.json shouldBe responseBody

      }
    }


    /*    "return a 404 status code" when {

      "any valid request is made but no income source has been found" in new CreateBFLossControllerTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.serviceSuccess(nino)
        }

        val response: WSResponse = await(request().post(requestBody))
        response.status shouldBe Status.NOT_FOUND
        response.json shouldBe Json.toJson(NotFoundError)
      }
    }

    "return 500 (Internal Server Error)" when {

      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      retrieveObligationsErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      retrieveObligationsErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 400 (Bad Request)" when {
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_IDNUMBER", Status.BAD_REQUEST, NinoFormatError)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_DATE_TO", Status.BAD_REQUEST, TaxYearFormatError)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_DATE_FROM", Status.BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_DATE_FROM", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_DATE_FROM", Status.BAD_REQUEST, RuleTaxYearRangeExceededError)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_DATE_FROM", Status.BAD_REQUEST, RuleTypeOfLossUnsupported)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_DATE_FROM", Status.BAD_REQUEST, RuleInvalidSelfEmploymentId)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_DATE_FROM", Status.BAD_REQUEST, RulePropertySelfEmploymentId)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_DATE_FROM", Status.BAD_REQUEST, AmountFormatError)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_DATE_FROM", Status.BAD_REQUEST, RuleInvalidLossAmount)

      BadRequestError
      | NinoFormatError
        | TaxYearFormatError
        | RuleIncorrectOrEmptyBodyError
        | RuleTaxYearNotSupportedError
        | RuleTaxYearRangeExceededError
        | RuleTypeOfLossUnsupported
        | RuleInvalidSelfEmploymentId
        | RulePropertySelfEmploymentId
        | AmountFormatError
        | RuleInvalidLossAmount
    }

    "return 404 NOT FOUND" when {
      retrieveObligationsErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
    }
  }*/
  }
}
