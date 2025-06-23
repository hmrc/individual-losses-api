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

package v6.lossClaim.retrieve.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.ClaimIdFormatError
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors._
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_RetrieveLossClaimHipISpec extends IntegrationBaseSpec {

  val businessId   = "XKIS00000000988"
  val lastModified = "2018-07-13T12:13:48.763Z"

  val downstreamResponseJson: JsValue = Json.parse(s"""
       |{
       |  "incomeSourceId": "$businessId",
       |  "reliefClaimed": "CF",
       |  "taxYearClaimedFor": 2020,
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
         |    "sequence": 1
         |}
      """.stripMargin)

    def downstreamUrl: String = s"/itsd/income-sources/claims-for-relief/$nino/$claimId"

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

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(s"/$nino/loss-claims/$claimId")
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.6.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

  "Calling the retrieve LossClaim endpoint" should {

    "return a 200 status code" when {
      "any valid request is made" in new Test {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl, Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
        response.json shouldBe responseJson
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
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

    "handle errors according to spec" when {
      def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $downstreamCode error" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl, downstreamStatus, errorBody(downstreamCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(Status.BAD_REQUEST, "1215", Status.BAD_REQUEST, NinoFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "1220", Status.BAD_REQUEST, ClaimIdFormatError)
      serviceErrorTest(Status.NOT_FOUND, "5010", Status.NOT_FOUND, NotFoundError)
    }

  }

}
