/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import play.api.http.HeaderNames.ACCEPT
import v1.models.errors.{DownstreamError, MtdError, NinoFormatError, NotFoundError, TaxYearFormatError}
import v1.stubs.{DesStub, MtdIdLookupStub}

class AmendLossClaimsOrderControllerISpec extends IntegrationBaseSpec {

  val correlationId = "X-123"

  val requestJson: JsValue = Json.parse(
    s"""
       |{
       |    "typeOfClaim": "carry-forward"
       |}
      """.stripMargin)

  private trait Test {

    val nino    = "AA123456A"
    val taxYear = "2019-20"

    val responseJson: JsValue = Json.parse(s"""
                                              |{
                                              |    "links": [
                                              |      {
                                              |      "href": "/individuals/losses/$nino/loss-claims/order",
                                              |      "method": "GET",
                                              |      "rel": "self"
                                              |      }
                                              |    ]
                                              |}
      """.stripMargin)

    def uri: String    = s"/$nino/loss-claims/order"
    def desUrl: String = s"/income-tax/claims-for-relief/$nino/preferences/$taxYear"

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "des message"
         |      }
      """.stripMargin

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

  }

  "Calling the amend LossClaim endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.PUT, desUrl, Status.OK)
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe Status.OK
        response.json shouldBe responseJson
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "handle errors according to spec" when {
      def serviceErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"des returns an $desCode error" in new Test {

          override def setupStubs(): StubMapping = {
            MtdIdLookupStub.ninoFound(nino)
            DesStub.onError(DesStub.PUT, desUrl, desStatus, errorBody(desCode))
          }

          val response: WSResponse = await(request().post(requestJson))
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError)
      serviceErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      serviceErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      serviceErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String, requestTaxYear: String, requestBody: JsValue,
                              expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String     = requestNino
          override val taxYear: String  = requestTaxYear

          override def setupStubs(): StubMapping = {
            MtdIdLookupStub.ninoFound(requestNino)
          }

          val response: WSResponse = await(request().post(requestBody))
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      validationErrorTest("BADNINO", "2019-20", requestJson, Status.BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", "BadDate", requestJson, Status.BAD_REQUEST, TaxYearFormatError)
    }
  }

}
