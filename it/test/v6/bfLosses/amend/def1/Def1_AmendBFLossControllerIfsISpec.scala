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
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors.*
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_AmendBFLossControllerIfsISpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, Any] =
    Map("feature-switch.ifs_hip_migration_1501.enabled" -> false) ++ super.servicesConfig

  val lossAmount = 2345.67

  val downstreamResponseJson: JsValue = Json.parse(s"""
                                                      |{
                                                      |    "incomeSourceId": "XBIS12345678910",
                                                      |    "lossType": "INCOME",
                                                      |    "broughtForwardLossAmount": $lossAmount,
                                                      |    "taxYear": "2022",
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
       |      {
       |        "code": "$code",
       |        "reason": "downstream message"
       |      }
      """.stripMargin

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

    def url: String           = s"/$nino/brought-forward-losses/$lossId/tax-year/$taxYear/change-loss-amount"
    def downstreamUrl: String = s"/income-tax/brought-forward-losses/$nino/$downstreamTaxYear/$lossId"

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
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUrl, Status.OK, downstreamResponseJson)
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
          ("AA123456A", "XAIS12345678910", "2020-21", JsObject.empty, BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
        )

        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "downstream service error" when {
        def serviceErrorTest(ifsStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $ifsStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.PUT, downstreamUrl, ifsStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request().post(requestJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = List(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_LOSS_ID", BAD_REQUEST, LossIdFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError),
          (CONFLICT, "CONFLICT", BAD_REQUEST, RuleLossAmountNotChanged),
          (UNPROCESSABLE_ENTITY, "OUTSIDE_AMENDMENT_WINDOW", BAD_REQUEST, RuleOutsideAmendmentWindow),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }

}
