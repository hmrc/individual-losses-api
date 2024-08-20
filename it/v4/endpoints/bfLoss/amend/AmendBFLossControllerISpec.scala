/*
 * Copyright 2023 HM Revenue & Customs
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

package v4.endpoints.bfLoss.amend

import api.hateoas.HateoasLinks
import api.models.errors._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import support.IntegrationBaseSpec
import support.stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}

class AmendBFLossControllerISpec extends IntegrationBaseSpec {

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

  object Hateoas extends HateoasLinks

  private trait Test {

    val nino   = "AA123456A"
    val lossId = "AAZZ1234567890a"

    val responseJson: JsValue = Json.parse(s"""
         |{
         |    "businessId": "XBIS12345678910",
         |    "typeOfLoss": "self-employment",
         |    "lossAmount": 2345.67,
         |    "taxYearBroughtForwardFrom": "2021-22",
         |    "lastModified": "2022-07-13T12:13:48.763Z",
         |    "links": [
         |      {
         |        "href": "/individuals/losses/AA123456A/brought-forward-losses/AAZZ1234567890a",
         |        "rel": "self",
         |        "method": "GET"
         |      },
         |      {
         |        "href": "/individuals/losses/AA123456A/brought-forward-losses/AAZZ1234567890a/change-loss-amount",
         |        "rel": "amend-brought-forward-loss",
         |        "method": "POST"
         |      },
         |      {
         |        "href": "/individuals/losses/AA123456A/brought-forward-losses/AAZZ1234567890a",
         |        "rel": "delete-brought-forward-loss",
         |        "method": "DELETE"
         |      }
         |     ]
         |}
      """.stripMargin)

    def url: String    = s"/$nino/brought-forward-losses/$lossId/change-loss-amount"
    def ifsUrl: String = s"/income-tax/brought-forward-losses/$nino/$lossId"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(url)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.4.0+json"),
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
          DownstreamStub.onSuccess(DownstreamStub.PUT, ifsUrl, Status.OK, downstreamResponseJson)
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
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {

            override val nino: String   = requestNino
            override val lossId: String = requestLossId

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
          ("AA1123A", "XAIS12345678910", requestJson, BAD_REQUEST, NinoFormatError),
          ("AA123456A", "XAIS1234dfxgchjbn5678910", requestJson, BAD_REQUEST, LossIdFormatError),
          ("AA123456A", "XAIS12345678910", invalidRequestJson, BAD_REQUEST, ValueFormatError.withPath("/lossAmount")),
          ("AA123456A", "XAIS12345678910", JsObject.empty, BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
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
              DownstreamStub.onError(DownstreamStub.PUT, ifsUrl, ifsStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request().post(requestJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = List(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_LOSS_ID", BAD_REQUEST, LossIdFormatError),
          (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError),
          (CONFLICT, "CONFLICT", BAD_REQUEST, RuleLossAmountNotChanged),
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
