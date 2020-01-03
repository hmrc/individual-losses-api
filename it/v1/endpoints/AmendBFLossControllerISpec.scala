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
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.hateoas.HateoasLinks
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class AmendBFLossControllerISpec extends IntegrationBaseSpec {

  val lossAmount = 531.99

  val desResponseJson: JsValue = Json.parse(
    s"""
       |{
       |"incomeSourceId": "XKIS00000000988",
       |"lossType": "INCOME",
       |"broughtForwardLossAmount": $lossAmount,
       |"taxYear": "2020",
       |"lossId": "AAZZ1234567890a",
       |"submissionDate": "2018-07-13T12:13:48.763Z"
       |}
      """.stripMargin)

  val requestJson: BigDecimal => JsValue = lossAmount => Json.parse(
    s"""
       |{
       |    "lossAmount": $lossAmount
       |}
      """.stripMargin)

  def errorBody(code: String): String =
    s"""
       |      {
       |        "code": "$code",
       |        "reason": "des message"
       |      }
      """.stripMargin

  object Hateoas extends HateoasLinks

  private trait Test {

    val nino = "AA123456A"
    val lossId = "AAZZ1234567890a"
    val correlationId = "X-123"
    val selfEmploymentId = "XKIS00000000988"
    val taxYear = "2019-20"
    val typeOfLoss = "self-employment"

    val responseJson: JsValue = Json.parse(
      s"""
         |{
         |    "selfEmploymentId": "XKIS00000000988",
         |    "typeOfLoss": "self-employment",
         |    "taxYear": "2019-20",
         |    "lossAmount": $lossAmount,
         |    "lastModified": "2018-07-13T12:13:48.763Z",
         |    "links": [{
         |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
         |      "method": "GET",
         |      "rel": "self"
         |    }
         |    ]
         |}
      """.stripMargin)

    def setupStubs(): StubMapping

    def uri: String

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

  }

  "Calling the amend BFLoss endpoint" should {

    trait AmendBFLossControllerTest extends Test {
      def uri: String = s"/$nino/brought-forward-losses/$lossId/change-loss-amount"
      def desUrl: String = s"/income-tax/brought-forward-losses/$nino/$lossId"
    }

    "return a 200 status code" when {

      "any valid request is made" in new AmendBFLossControllerTest() {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.PUT, desUrl, Status.OK, desResponseJson)
        }

        val response: WSResponse = await(request().post(requestJson(531.99)))
        response.status shouldBe Status.OK
        response.json shouldBe responseJson
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }


    "return 500 (Internal Server Error)" when {

      amendErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      amendErrorTest(Status.BAD_REQUEST, "UNEXPECTED_DES_ERROR_CODE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      amendErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      amendErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 404 NOT FOUND" when {
      amendErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
    }

    "return 409 CONFLICT" when {
      amendErrorTest(Status.CONFLICT, "CONFLICT", Status.FORBIDDEN, RuleLossAmountNotChanged)
    }

    def amendErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
      s"des returns an $desCode error" in new AmendBFLossControllerTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onError(DesStub.PUT, desUrl, desStatus, errorBody(desCode))
        }

        val response: WSResponse = await(request().post(requestJson(531.99)))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return 400 (Bad Request)" when {

      amendBFLossValidationErrorTest("BADNINO", requestJson(531.99), Status.BAD_REQUEST, NinoFormatError)
      amendBFLossValidationErrorTest("AA123456A", Json.toJson("dsdfs"), Status.BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
      amendBFLossValidationErrorTest("AA123456A", requestJson(-3234.99), Status.BAD_REQUEST, RuleInvalidLossAmount)
      amendBFLossValidationErrorTest("AA123456A",requestJson(99999999999.999), Status.BAD_REQUEST, AmountFormatError)
    }


    def amendBFLossValidationErrorTest(requestNino: String, requestBody: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
      s"validation fails with ${expectedBody.code} error" in new AmendBFLossControllerTest {

        override val nino: String = requestNino

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().post(requestBody))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }
  }
}
