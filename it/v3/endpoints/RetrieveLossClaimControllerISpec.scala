/*
 * Copyright 2022 HM Revenue & Customs
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

package v3.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }
import support.V3IntegrationBaseSpec
import v3.models.errors._
import v3.stubs.{ AuditStub, AuthStub, IfsStub, MtdIdLookupStub }

class RetrieveLossClaimControllerISpec extends V3IntegrationBaseSpec {

  val correlationId = "X-123"
  val businessId    = "XKIS00000000988"
  val lastModified  = "2018-07-13T12:13:48.763Z"

  val downstreamResponseJson: JsValue = Json.parse(s"""
       |{
       |  "incomeSourceId": "$businessId",
       |  "reliefClaimed": "CF",
       |  "taxYearClaimedFor": "2020",
       |  "claimId": "notUsed",
       |  "sequence": 1,
       |  "submissionDate": "$lastModified"
       |}
      """.stripMargin)

  private trait Test {

    val nino    = "AA123456A"
    val claimId = "AAZZ1234567890a"

    val responseJson: JsValue = Json.parse(s"""
         |{
         |    "businessId": "$businessId",
         |    "typeOfLoss": "self-employment",
         |    "typeOfClaim": "carry-forward",
         |    "taxYearClaimedFor": "2019-20",
         |    "lastModified":"$lastModified",
         |    "sequence": 1,
         |    "links": [{
         |      "href": "/individuals/losses/$nino/loss-claims/$claimId",
         |      "method": "GET",
         |      "rel": "self"
         |    },
         |    {
         |      "href": "/individuals/losses/$nino/loss-claims/$claimId",
         |      "method": "DELETE",
         |      "rel": "delete-loss-claim"
         |    },{
         |      "href": "/individuals/losses/$nino/loss-claims/$claimId/change-type-of-claim",
         |      "method": "POST",
         |      "rel": "amend-loss-claim"
         |    }
         |    ]
         |}
      """.stripMargin)

    def uri: String    = s"/$nino/loss-claims/$claimId"
    def ifsUrl: String = s"/income-tax/claims-for-relief/$nino/$claimId"

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
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.3.0+json"))
    }
  }

  "Calling the retrieve LossClaim endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          IfsStub.onSuccess(IfsStub.GET, ifsUrl, Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
        response.json shouldBe responseJson
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "handle errors according to spec" when {
      def serviceErrorTest(ifsStatus: Int, ifsCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $ifsCode error" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            IfsStub.onError(IfsStub.GET, ifsUrl, ifsStatus, errorBody(ifsCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_CLAIM_ID", Status.BAD_REQUEST, ClaimIdFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_CORRELATIONID", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      serviceErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
      serviceErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      serviceErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String, requestClaimId: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String    = requestNino
          override val claimId: String = requestClaimId

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
      validationErrorTest("AA123456A", "BADClaimId", Status.BAD_REQUEST, ClaimIdFormatError)
    }

  }
}
