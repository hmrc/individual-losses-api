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
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors.*
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_AmendLossClaimTypeHipISpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, Any] =
    Map("feature-switch.ifs_hip_migration_1506.enabled" -> true, "feature-switch.ifs_hip_migration_1508.enabled" -> true) ++ super.servicesConfig

  val downstreamResponseJson: JsValue = Json.parse(s"""
      |{
      |  "incomeSourceId": "XKIS00000000988",
      |  "reliefClaimed": "CF",
      |  "taxYearClaimedFor": 2026,
      |  "claimId": "AT0000000000001",
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

    val nino                        = "AA123456A"
    val claimId                     = "AAZZ1234567890a"
    val taxYearClaimedFor           = "2025-26"
    val downstreamTaxYearClaimedFor = "25-26"

    val responseJson: JsValue = Json.parse(s"""
      |{
      |  "businessId": "XKIS00000000988",
      |  "typeOfLoss": "self-employment",
      |  "typeOfClaim": "carry-forward",
      |  "taxYearClaimedFor": "2025-26",
      |  "lastModified":"2018-07-13T12:13:48.763Z",
      |  "sequence": 1
      |}
      """.stripMargin)

    def uri: String                           = s"/$nino/loss-claims/$claimId/tax-year/$taxYearClaimedFor/change-type-of-claim"
    def hipUrl: String                        = s"/itsd/income-sources/claims-for-relief/$nino/$claimId"
    val downstreamParams: Map[String, String] = Map("taxYear" -> downstreamTaxYearClaimedFor)

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

    def request(uri: String = uri): WSRequest = {
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
          DownstreamStub.onSuccess(DownstreamStub.PUT, hipUrl, downstreamParams, OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().post(requestJson))
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "handle errors according to spec" when {
      def serviceErrorTest(hipStatus: Int, hipCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $hipCode error" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.PUT, hipUrl, downstreamParams, hipStatus, errorBody(hipCode))
          }

          val response: WSResponse = await(request().post(requestJson))
          response.json shouldBe Json.toJson(expectedBody)
          response.status shouldBe expectedStatus
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(BAD_REQUEST, "1117", BAD_REQUEST, TaxYearClaimedForFormatError)
      serviceErrorTest(BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError)
      serviceErrorTest(BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(BAD_REQUEST, "1220", BAD_REQUEST, ClaimIdFormatError)
      serviceErrorTest(NOT_FOUND, "5010", NOT_FOUND, NotFoundError)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "1000", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "1105", BAD_REQUEST, RuleTypeOfClaimInvalid)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "1127", BAD_REQUEST, RuleCSFHLClaimNotSupportedError)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "1228", BAD_REQUEST, RuleClaimTypeNotChanged)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "4200", BAD_REQUEST, RuleOutsideAmendmentWindow)
      serviceErrorTest(NOT_IMPLEMENTED, "5000", BAD_REQUEST, RuleTaxYearNotSupportedError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestClaimId: String,
                              requestBody: JsValue,
                              expectedStatus: Int,
                              expectedBody: MtdError,
                              requestTaxYearClaimedFor: String = "2025-26"): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String              = requestNino
          override val claimId: String           = requestClaimId
          override val taxYearClaimedFor: String = requestTaxYearClaimedFor

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(requestNino)
          }

          val response: WSResponse = await(request(uri).post(requestBody))
          response.json shouldBe Json.toJson(expectedBody)
          response.status shouldBe expectedStatus
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      validationErrorTest(
        requestNino = "BADNINO",
        requestClaimId = "AAZZ1234567890a",
        requestBody = requestJson,
        expectedStatus = BAD_REQUEST,
        expectedBody = NinoFormatError
      )
      
      validationErrorTest(
        requestNino = "AA123456A",
        requestClaimId = "BADClaimId",
        requestBody = requestJson,
        expectedStatus = BAD_REQUEST,
        expectedBody = ClaimIdFormatError
      )
      
      validationErrorTest(
        requestNino = "AA123456A",
        requestClaimId = "AAZZ1234567890a",
        requestBody = Json.obj(), expectedStatus = BAD_REQUEST,
        expectedBody = RuleIncorrectOrEmptyBodyError
      )
      
      validationErrorTest(
        requestNino = "AA123456A",
        requestClaimId = "AAZZ1234567890a",
        requestBody = Json.obj("typeOfClaim" -> "xxx"),
        expectedStatus = BAD_REQUEST,
        expectedBody = TypeOfClaimFormatError.withPath("/typeOfClaim")
      )

      validationErrorTest(
        requestNino = "AA123456A",
        requestClaimId = "AAZZ1234567890a",
        requestBody = Json.obj("typeOfClaim" -> "carry-forward"),
        expectedStatus = BAD_REQUEST,
        expectedBody = TaxYearClaimedForFormatError,
        requestTaxYearClaimedFor = "202324"
      )

      validationErrorTest(
        requestNino = "AA123456A",
        requestClaimId = "AAZZ1234567890a",
        requestBody = Json.obj("typeOfClaim" -> "carry-forward"),
        expectedStatus = BAD_REQUEST,
        expectedBody = RuleTaxYearRangeInvalidError,
        requestTaxYearClaimedFor = "2018-24"
      )

      validationErrorTest(
        requestNino = "AA123456A",
        requestClaimId = "AAZZ1234567890a",
        requestBody = Json.obj("typeOfClaim" -> "carry-forward"),
        expectedStatus = BAD_REQUEST,
        expectedBody = RuleTaxYearNotSupportedError,
        requestTaxYearClaimedFor = "2017-18"
      )

      validationErrorTest(
        requestNino = "AA123456A",
        requestClaimId = "AAZZ1234567890a",
        requestBody = Json.obj("typeOfClaim" -> "carry-forward"),
        expectedStatus = BAD_REQUEST,
        expectedBody = RuleTaxYearForVersionNotSupportedError,
        requestTaxYearClaimedFor = "2026-27"
      )
    }
  }

}
