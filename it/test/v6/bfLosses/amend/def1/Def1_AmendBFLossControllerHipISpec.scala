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

package v6.bfLosses.amend.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.{LossIdFormatError, RuleLossAmountNotChanged, RuleOutsideAmendmentWindow}
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.http.Status.*
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors.*
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_AmendBFLossControllerHipISpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, Any] =
    Map("feature-switch.ifs_hip_migration_1501.enabled" -> true) ++ super.servicesConfig

  val lossAmount = 2345.67

  val downstreamResponseJson: JsValue = Json.parse(s"""
                                                      |{
                                                      |    "incomeSourceId": "XBIS12345678910",
                                                      |    "lossType": "INCOME",
                                                      |    "broughtForwardLossAmount": $lossAmount,
                                                      |    "taxYearBroughtForwardFrom": 2022,
                                                      |    "lossId": "AAZZ1234567890A",
                                                      |    "submissionDate": "2022-07-13T12:13:48.763Z"
                                                      |}
      """.stripMargin)

  val requestJson: JsValue = Json.parse(s"""
                                           |{
                                           |    "lossAmount": $lossAmount
                                           |}
      """.stripMargin)

  val invalidRequestJson: JsValue = Json.parse(s"""
                                                  |{
                                                  |    "lossAmount": 23.2714
                                                  |}
      """.stripMargin)

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

  private trait Test {

    val nino              = "AA123456A"
    val lossId            = "AAZZ1234567890a"
    val taxYear           = "2020-21"
    val downstreamTaxYear = "20-21"

    val responseJson: JsValue = Json.parse(s"""
                                              |{
                                              |    "businessId": "XBIS12345678910",
                                              |    "typeOfLoss": "self-employment",
                                              |    "lossAmount": 2345.67,
                                              |    "taxYearBroughtForwardFrom": "2021-22",
                                              |    "lastModified": "2022-07-13T12:13:48.763Z"
                                              |}
      """.stripMargin)

    def url: String                      = s"/$nino/brought-forward-losses/$lossId/tax-year/$taxYear/change-loss-amount"
    def downstreamUrl: String            = s"/itsd/income-sources/brought-forward-losses/$nino/$lossId"
    val queryParams: Map[String, String] = Map("taxYear" -> downstreamTaxYear)

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(url)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.6.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

  "Calling the amend BFLoss endpoint" should {
    "return a 200 status code" when {

      "a valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUrl, queryParams, Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe Status.OK
        response.json shouldBe responseJson
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return errors as per the spec" when {
      "validation error occurs" when {
        def validationErrorTest(requestNino: String,
                                requestLossId: String,
                                requestTaxYear: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {

            override val nino: String    = requestNino
            override val lossId: String  = requestLossId
            override val taxYear: String = requestTaxYear

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().post(requestBody))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = List(
          ("AA1123A", "XAIS12345678910", "2020-21", requestJson, BAD_REQUEST, NinoFormatError),
          ("AA123456A", "XAIS1234dfxgchjbn5678910", "2020-21", requestJson, BAD_REQUEST, LossIdFormatError),
          ("AA123456A", "XAIS12345678910", "2020-2021", requestJson, BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "XAIS12345678910", "2020-21", invalidRequestJson, BAD_REQUEST, ValueFormatError.withPath("/lossAmount")),
          ("AA123456A", "XAIS12345678910", "2020-21", JsObject.empty, BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123456A", "XAIS12345678910", "2017-18", requestJson, BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "XAIS12345678910", "2026-27", requestJson, BAD_REQUEST, RuleTaxYearForVersionNotSupportedError)
        )

        input.foreach(validationErrorTest.tupled)
      }

      "downstream service error" when {
        def serviceErrorTest(ifsStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $ifsStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.PUT, downstreamUrl, queryParams, ifsStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request().post(requestJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = List(
          (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "1219", BAD_REQUEST, LossIdFormatError),
          (BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError),
          (NOT_FOUND, "5010", NOT_FOUND, NotFoundError),
          (CONFLICT, "1225", BAD_REQUEST, RuleLossAmountNotChanged),
          (BAD_REQUEST, "1000", INTERNAL_SERVER_ERROR, InternalError),
          (INTERNAL_SERVER_ERROR, "1000", INTERNAL_SERVER_ERROR, InternalError),
          (SERVICE_UNAVAILABLE, "1000", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "4200", BAD_REQUEST, RuleOutsideAmendmentWindow)
        )

        input.foreach(serviceErrorTest.tupled)
      }
    }
  }

}
