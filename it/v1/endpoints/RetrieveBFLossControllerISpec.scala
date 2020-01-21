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

class RetrieveBFLossControllerISpec extends IntegrationBaseSpec {

  val correlationId = "X-123"

  val lossAmount = 531.99

  object Hateoas extends HateoasLinks

  val desResponseJson: JsValue = Json.parse(s"""
       |{
       |"incomeSourceId": "XKIS00000000988",
       |"lossType": "INCOME",
       |"broughtForwardLossAmount": $lossAmount,
       |"taxYear": "2020",
       |"lossId": "AAZZ1234567890a",
       |"submissionDate": "2018-07-13T12:13:48.763Z"
       |}
      """.stripMargin)

  private trait Test {

    val nino   = "AA123456A"
    val lossId = "AAZZ1234567890a"

    val responseJson: JsValue = Json.parse(s"""
         |{
         |    "selfEmploymentId": "XKIS00000000988",
         |    "typeOfLoss": "self-employment",
         |    "taxYear": "2019-20",
         |    "lossAmount": $lossAmount,
         |    "lastModified":"2018-07-13T12:13:48.763Z",
         |    "links": [{
         |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
         |      "method": "GET",
         |      "rel": "self"
         |    },
         |    {
         |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
         |      "method": "DELETE",
         |      "rel": "delete-brought-forward-loss"
         |    },{
         |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId/change-loss-amount",
         |      "method": "POST",
         |      "rel": "amend-brought-forward-loss"
         |    }
         |    ]
         |}
      """.stripMargin)

    def uri: String    = s"/$nino/brought-forward-losses/$lossId"
    def desUrl: String = s"/income-tax/brought-forward-losses/$nino/$lossId"

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

  "Calling the retrieve BFLoss endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUrl, Status.OK, desResponseJson)
        }

        val response: WSResponse = await(request().get())
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
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DesStub.onError(DesStub.GET, desUrl, desStatus, errorBody(desCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_LOSS_ID", Status.BAD_REQUEST, LossIdFormatError)
      serviceErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
      serviceErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      serviceErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
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
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      validationErrorTest("BADNINO", "AAZZ1234567890a", Status.BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", "BADLOSSID", Status.BAD_REQUEST, LossIdFormatError)
    }

  }
}
