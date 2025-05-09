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

package v4.endpoints.lossClaim.delete

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.ClaimIdFormatError
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers._
import shared.models.errors._
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class DeleteLossClaimItsaISpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, Any] =
    Map("feature-switch.hipItsa_hipItsd_migration_1509.enabled" -> false) ++ super.servicesConfig

  override def servicesConfig: Map[String, Any] =
    Map("feature-switch.ifs_hip_migration_1508.enabled" -> false) ++ super.servicesConfig

  val retrieveDownstreamResponseJson: JsValue = Json.parse(
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

  private trait Test {

    val nino    = "AA123456A"
    val claimId = "AAZZ1234567890a"

    private def uri: String           = s"/$nino/loss-claims/$claimId"
    def deleteDownstreamUrl: String   = s"/itsa/income-tax/v1/claims-for-relief/$nino/19-20/$claimId"
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
          (ACCEPT, "application/vnd.hmrc.4.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

  "Calling the delete Loss Claim endpoint" should {

    "return a 204 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, retrieveDownstreamUrl, OK, retrieveDownstreamResponseJson)
          DownstreamStub.onSuccess(DownstreamStub.DELETE, deleteDownstreamUrl, NO_CONTENT, JsObject.empty)
        }

        val response: WSResponse = await(request().delete())
        response.status shouldBe NO_CONTENT
        response.header("X-CorrelationId").nonEmpty shouldBe true
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

          val response: WSResponse = await(request().delete())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }

      def serviceDeleteErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit =
        s"downstream returns a code $downstreamCode error from the delete connector call" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onSuccess(DownstreamStub.GET, retrieveDownstreamUrl, OK, retrieveDownstreamResponseJson)
            DownstreamStub.onError(DownstreamStub.DELETE, deleteDownstreamUrl, downstreamStatus, errorBody(downstreamCode))
          }

          val response: WSResponse = await(request().delete())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }

      serviceRetrieveErrorTest(BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError)
      serviceRetrieveErrorTest(BAD_REQUEST, "INVALID_CLAIM_ID", BAD_REQUEST, ClaimIdFormatError)
      serviceRetrieveErrorTest(BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError)
      serviceRetrieveErrorTest(NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError)
      serviceRetrieveErrorTest(INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
      serviceRetrieveErrorTest(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)

      serviceDeleteErrorTest(BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError)
      serviceDeleteErrorTest(BAD_REQUEST, "INVALID_CLAIM_ID", BAD_REQUEST, ClaimIdFormatError)
      serviceDeleteErrorTest(BAD_REQUEST, "UNEXPECTED_DOWNSTREAM_ERROR_CODE", INTERNAL_SERVER_ERROR, InternalError)
      serviceDeleteErrorTest(NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError)
      serviceDeleteErrorTest(INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
      serviceDeleteErrorTest(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
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

          val response: WSResponse = await(request().delete())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      validationErrorTest("BADNINO", "AAZZ1234567890a", BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", "BADCLAIMID", BAD_REQUEST, ClaimIdFormatError)
    }

  }

}
