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

package v5.lossClaim.amendType.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*
import shared.models.errors.*
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_AmendLossClaimTypeIfsISpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, Any] =
    Map("feature-switch.ifs_hip_migration_1506.enabled" -> false, "feature-switch.ifs_hip_migration_1508.enabled" -> false) ++ super.servicesConfig

  val downstreamResponseJson: JsValue = Json.parse(
    """
      |{
      |  "incomeSourceId": "XKIS00000000988",
      |  "reliefClaimed": "CF",
      |  "taxYearClaimedFor": "2020",
      |  "claimId": "notUsed",
      |  "sequence": 1,
      |  "submissionDate": "2018-07-13T12:13:48.763Z"
      |}
    """.stripMargin
  )

  val requestJson: JsValue = Json.parse(
    """
      |{
      |  "typeOfClaim": "carry-forward"
      |}
    """.stripMargin
  )

  private trait Test {

    val nino    = "AA123456A"
    val claimId = "AAZZ1234567890a"

    val responseJson: JsValue = Json.parse(
      """
        |{
        |  "businessId": "XKIS00000000988",
        |  "typeOfLoss": "self-employment",
        |  "typeOfClaim": "carry-forward",
        |  "taxYearClaimedFor": "2019-20",
        |  "lastModified":"2018-07-13T12:13:48.763Z",
        |  "sequence": 1
        |}
      """.stripMargin
    )

    private def uri: String           = s"/$nino/loss-claims/$claimId/change-type-of-claim"
    def amendDownstreamUrl: String    = s"/income-tax/claims-for-relief/$nino/19-20/$claimId"
    def retrieveDownstreamUrl: String = s"/income-tax/claims-for-relief/$nino/$claimId"

    def errorBody(code: String): String =
      s"""
        |{
        |  "code": "$code",
        |  "reason": "downstream message"
        |}
      """.stripMargin

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.5.0+json"),
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
          DownstreamStub.onSuccess(DownstreamStub.GET, retrieveDownstreamUrl, OK, downstreamResponseJson)
          DownstreamStub.onSuccess(DownstreamStub.PUT, amendDownstreamUrl, OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().post(requestJson))
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "handle errors according to spec" when {
      def serviceRetrieveErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit =
        s"downstream returns a code $downstreamCode error from the retrieve connector call" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.GET, retrieveDownstreamUrl, downstreamStatus, errorBody(downstreamCode))
          }

          val response: WSResponse = await(request().post(requestJson))
          response.json shouldBe Json.toJson(expectedBody)
          response.status shouldBe expectedStatus
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }

      def serviceAmendErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit =
        s"downstream returns a code $downstreamCode error from the amend connector call" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onSuccess(DownstreamStub.GET, retrieveDownstreamUrl, OK, downstreamResponseJson)
            DownstreamStub.onError(DownstreamStub.PUT, amendDownstreamUrl, downstreamStatus, errorBody(downstreamCode))
          }

          val response: WSResponse = await(request().post(requestJson))
          response.json shouldBe Json.toJson(expectedBody)
          response.status shouldBe expectedStatus
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }

      serviceRetrieveErrorTest(BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError)
      serviceRetrieveErrorTest(BAD_REQUEST, "INVALID_CLAIM_ID", BAD_REQUEST, ClaimIdFormatError)
      serviceRetrieveErrorTest(BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError)
      serviceRetrieveErrorTest(NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError)
      serviceRetrieveErrorTest(INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
      serviceRetrieveErrorTest(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)

      serviceAmendErrorTest(BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError)
      serviceAmendErrorTest(BAD_REQUEST, "INVALID_CLAIM_ID", BAD_REQUEST, ClaimIdFormatError)
      serviceAmendErrorTest(BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError)
      serviceAmendErrorTest(BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError)
      serviceAmendErrorTest(UNPROCESSABLE_ENTITY, "INVALID_CLAIM_TYPE", BAD_REQUEST, RuleTypeOfClaimInvalid)
      serviceAmendErrorTest(UNPROCESSABLE_ENTITY, "CSFHL_CLAIM_NOT_SUPPORTED", BAD_REQUEST, RuleCSFHLClaimNotSupportedError)
      serviceAmendErrorTest(CONFLICT, "CONFLICT", BAD_REQUEST, RuleClaimTypeNotChanged)
      serviceAmendErrorTest(NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError)
      serviceAmendErrorTest(INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
      serviceAmendErrorTest(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
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
