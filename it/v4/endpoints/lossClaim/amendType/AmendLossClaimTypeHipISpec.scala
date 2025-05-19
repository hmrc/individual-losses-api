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

package v4.endpoints.lossClaim.amendType

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.{
  ClaimIdFormatError,
  RuleCSFHLClaimNotSupportedError,
  RuleClaimTypeNotChanged,
  RuleOutsideAmendmentWindow,
  RuleTypeOfClaimInvalid,
  TypeOfClaimFormatError
}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers._
import shared.models.errors._
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class AmendLossClaimTypeHipISpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, Any] =
    Map("feature-switch.ifs_hip_migration_1506.enabled" -> true, "feature-switch.ifs_hip_migration_1508.enabled" -> true) ++ super.servicesConfig

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

  val amendDownstreamResponseJson: JsValue = Json.parse(
    """
      |{
      |  "incomeSourceId": "XKIS00000000988",
      |  "reliefClaimed": "CF",
      |  "taxYearClaimedFor": 2020,
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
      s"""
        |{
        |  "businessId": "XKIS00000000988",
        |  "typeOfLoss": "self-employment",
        |  "typeOfClaim": "carry-forward",
        |  "taxYearClaimedFor": "2019-20",
        |  "lastModified":"2018-07-13T12:13:48.763Z",
        |  "sequence": 1,
        |  "links": [
        |    {
        |      "href": "/individuals/losses/$nino/loss-claims/$claimId",
        |      "method": "GET",
        |      "rel": "self"
        |    },
        |    {
        |      "href": "/individuals/losses/$nino/loss-claims/$claimId",
        |      "method": "DELETE",
        |      "rel": "delete-loss-claim"
        |    },
        |
        |    {
        |      "href": "/individuals/losses/$nino/loss-claims/$claimId/change-type-of-claim",
        |      "method": "POST",
        |      "rel": "amend-loss-claim"
        |    }
        |  ]
        |}
      """.stripMargin
    )

    private def uri: String           = s"/$nino/loss-claims/$claimId/change-type-of-claim"
    def amendDownstreamUrl: String    = s"/itsd/income-sources/claims-for-relief/$nino/$claimId"
    def retrieveDownstreamUrl: String = s"/itsd/income-sources/claims-for-relief/$nino/$claimId"

    def errorBody(code: String): String =
      s"""
         |[
         |  {
         |    "errorCode": "$code",
         |    "errorDescription": "error message"
         |  }
         |]
         |""".stripMargin

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

  "Calling the amend LossClaim endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, retrieveDownstreamUrl, OK, retrieveDownstreamResponseJson)
          DownstreamStub.onSuccess(DownstreamStub.PUT, amendDownstreamUrl, OK, amendDownstreamResponseJson)
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
            DownstreamStub.onSuccess(DownstreamStub.GET, retrieveDownstreamUrl, OK, retrieveDownstreamResponseJson)
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

      serviceAmendErrorTest(BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError)
      serviceAmendErrorTest(BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError)
      serviceAmendErrorTest(BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, InternalError)
      serviceAmendErrorTest(BAD_REQUEST, "1220", BAD_REQUEST, ClaimIdFormatError)
      serviceAmendErrorTest(NOT_FOUND, "5010", NOT_FOUND, NotFoundError)
      serviceAmendErrorTest(UNPROCESSABLE_ENTITY, "1000", INTERNAL_SERVER_ERROR, InternalError)
      serviceAmendErrorTest(UNPROCESSABLE_ENTITY, "1105", BAD_REQUEST, RuleTypeOfClaimInvalid)
      serviceAmendErrorTest(UNPROCESSABLE_ENTITY, "1127", BAD_REQUEST, RuleCSFHLClaimNotSupportedError)
      serviceAmendErrorTest(UNPROCESSABLE_ENTITY, "1228", BAD_REQUEST, RuleClaimTypeNotChanged)
      serviceAmendErrorTest(UNPROCESSABLE_ENTITY, "4200", BAD_REQUEST, RuleOutsideAmendmentWindow)
      serviceAmendErrorTest(NOT_IMPLEMENTED, "5000", BAD_REQUEST, RuleTaxYearNotSupportedError)
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
