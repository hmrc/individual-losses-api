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

package v6.lossClaim.amendType.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors._
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors._
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_AmendLossClaimTypeISpec extends IntegrationBaseSpec {

  val downstreamResponseJson: JsValue = Json.parse(s"""
       |{
       |  "incomeSourceId": "XKIS00000000988",
       |  "reliefClaimed": "CF",
       |  "taxYearClaimedFor": "2020",
       |  "claimId": "notUsed",
       |  "sequence": 1,
       |  "submissionDate": "2018-07-13T12:13:48.763Z"
       |}
      """.stripMargin)

  val requestJson: JsValue = Json.parse(s"""
       |{
       |    "typeOfClaim": "carry-forward"
       |}
      """.stripMargin)

  private trait Test {

    val nino              = "AA123456A"
    val claimId           = "AAZZ1234567890a"
    val taxYear           = "2020-21"
    val downstreamTaxYear = "20-21"

    val responseJson: JsValue = Json.parse(s"""
         |{
         |  "businessId": "XKIS00000000988",
         |  "typeOfLoss": "self-employment",
         |  "typeOfClaim": "carry-forward",
         |  "taxYearClaimedFor": "2019-20",
         |  "lastModified":"2018-07-13T12:13:48.763Z",
         |  "sequence": 1
         |}
      """.stripMargin)

    def uri: String    = s"/$nino/loss-claims/$claimId/tax-year/$taxYear/change-type-of-claim"
    def ifsUrl: String = s"/income-tax/claims-for-relief/$nino/$downstreamTaxYear/$claimId"

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
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.6.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

  "Calling the amend LossClaim endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, ifsUrl, OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().post(requestJson))
        response.json shouldBe responseJson
        response.status shouldBe OK
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
            DownstreamStub.onError(DownstreamStub.PUT, ifsUrl, ifsStatus, errorBody(ifsCode))
          }

          val response: WSResponse = await(request().post(requestJson))
          response.json shouldBe Json.toJson(expectedBody)
          response.status shouldBe expectedStatus
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError)
      serviceErrorTest(BAD_REQUEST, "INVALID_CLAIM_ID", BAD_REQUEST, ClaimIdFormatError)
      serviceErrorTest(BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "INVALID_CLAIM_TYPE", BAD_REQUEST, RuleTypeOfClaimInvalid)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "CSFHL_CLAIM_NOT_SUPPORTED", BAD_REQUEST, RuleCSFHLClaimNotSupportedError)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "OUTSIDE_AMENDMENT_WINDOW", BAD_REQUEST, RuleOutsideAmendmentWindow)
      serviceErrorTest(CONFLICT, "CONFLICT", BAD_REQUEST, RuleClaimTypeNotChanged)
      serviceErrorTest(NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError)
      serviceErrorTest(INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestClaimId: String,
                              requestBody: JsValue,
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String    = requestNino
          override val claimId: String = requestClaimId

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(requestNino)
          }

          val response: WSResponse = await(request().post(requestBody))
          response.json shouldBe Json.toJson(expectedBody)
          response.status shouldBe expectedStatus
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      validationErrorTest("BADNINO", "AAZZ1234567890a", requestJson, BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", "BADClaimId", requestJson, BAD_REQUEST, ClaimIdFormatError)
      validationErrorTest("AA123456A", "AAZZ1234567890a", Json.obj(), BAD_REQUEST, RuleIncorrectOrEmptyBodyError)

      validationErrorTest(
        "AA123456A",
        "AAZZ1234567890a",
        Json.obj("typeOfClaim" -> "xxx"),
        BAD_REQUEST,
        TypeOfClaimFormatError.withPath("/typeOfClaim"))
    }
  }

}
