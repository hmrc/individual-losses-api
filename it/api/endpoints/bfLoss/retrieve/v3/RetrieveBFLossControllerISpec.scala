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

package api.endpoints.bfLoss.retrieve.v3

import api.hateoas.HateoasLinks
import api.models.errors._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import support.V3V4IntegrationBaseSpec
import support.stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}

class RetrieveBFLossControllerISpec extends V3V4IntegrationBaseSpec {

  val lossAmount        = 531.99
  val businessId        = "XKIS00000000988"
  val lastModified      = "2018-07-13T12:13:48.763Z"
  val taxYear           = "2019-20"
  val downstreamTaxYear = "2020"

  object Hateoas extends HateoasLinks

  val downstreamResponseJson: JsValue = Json.parse(s"""
       |{
       |"incomeSourceId": "$businessId",
       |"lossType": "INCOME",
       |"broughtForwardLossAmount": $lossAmount,
       |"taxYear": "$downstreamTaxYear",
       |"lossId": "AAZZ1234567890a",
       |"submissionDate": "$lastModified"
       |}
      """.stripMargin)

  private trait Test {

    val nino   = "AA123456A"
    val lossId = "AAZZ1234567890a"

    val responseJson: JsValue = Json.parse(s"""
         |{
         |    "businessId": "$businessId",
         |    "typeOfLoss": "self-employment",
         |    "taxYearBroughtForwardFrom": "$taxYear",
         |    "lossAmount": $lossAmount,
         |    "lastModified":"$lastModified",
         |    "links": [{
         |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
         |      "method": "GET",
         |      "rel": "self"
         |    },
         |    {
         |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId/change-loss-amount",
         |      "method": "POST",
         |      "rel": "amend-brought-forward-loss"
         |    },
         |    {
         |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
         |      "method": "DELETE",
         |      "rel": "delete-brought-forward-loss"
         |    }
         |    ]
         |}
      """.stripMargin)

    def ifsUrl: String = s"/income-tax/brought-forward-losses/$nino/$lossId"

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "downstream message"
         |      }
      """.stripMargin

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(s"/$nino/brought-forward-losses/$lossId")
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.3.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

  "Calling the retrieve BFLoss endpoint" should {

    "return a 200 status code" when {
      "any valid request is made" in new Test {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, ifsUrl, Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String, requestLossId: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String   = requestNino
          override val lossId: String = requestLossId

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(requestNino)
          }

          val response: WSResponse = await(request().get())
          response.json shouldBe Json.toJson(expectedBody)
          response.status shouldBe expectedStatus
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      validationErrorTest("BADNINO", "AAZZ1234567890a", Status.BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", "BADLOSSID", Status.BAD_REQUEST, LossIdFormatError)
    }

    "handle errors according to spec" when {
      def serviceErrorTest(ifsStatus: Int, ifsCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $ifsCode error" in new Test {
          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.GET, ifsUrl, ifsStatus, errorBody(ifsCode))
          }

          val response: WSResponse = await(request().get())
          response.json shouldBe Json.toJson(expectedBody)
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_LOSS_ID", Status.BAD_REQUEST, LossIdFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_CORRELATIONID", Status.INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
      serviceErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, InternalError)
    }
  }

}
