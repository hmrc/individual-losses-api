/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.RuleOutsideAmendmentWindow
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status.*
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors.*
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class DeleteLossesAndClaimsControllerISpec extends IntegrationBaseSpec {

  private def errorBody(code: String): String =
    s"""
       |{
       |  "response": [
       |    {
       |      "errorCode": "$code",
       |      "errorDescription": "message"
       |    }
       |  ]
       |}
    """.stripMargin

  "Calling the Delete losses and claims endpoint" should {
    "return a 204 status code" when {
      "a valid request is made" in new Test {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.ninoFound(nino)
          AuthStub.authorised()

          DownstreamStub.onSuccess(
            method = DownstreamStub.DELETE,
            uri = downstreamUri,
            queryParams = Map("taxYear" -> "26-27"),
            status = NO_CONTENT,
            body = JsObject.empty
          )
        }

        val response: WSResponse = await(request().delete())
        response.status shouldBe NO_CONTENT
      }
    }

    "return error according to spec" when {
      "validation error" when {
        "validation error" when {
          def validationErrorTest(requestNino: String,
                                  requestBusinessId: String,
                                  requestTaxYear: String,
                                  expectedStatus: Int,
                                  expectedBody: MtdError): Unit = {
            s"validation fails with ${expectedBody.code} error" in new Test {

              override val nino: String       = requestNino
              override val businessId: String = requestBusinessId
              override val taxYear: String    = requestTaxYear

              override def setupStubs(): StubMapping = {
                AuditStub.audit()
                MtdIdLookupStub.ninoFound(nino)
                AuthStub.authorised()
              }

              val response: WSResponse = await(request().delete())
              response.status shouldBe expectedStatus
            }
          }

          val input = List(
            ("AA1123A", "XAIS12345678910", "2026-27", BAD_REQUEST, NinoFormatError),
            ("AA123456A", "invalid", "2026-27", BAD_REQUEST, BusinessIdFormatError),
            ("AA123456A", "XAIS12345678910", "invalid", BAD_REQUEST, TaxYearFormatError),
            ("AA123456A", "XAIS12345678910", "2025-27", BAD_REQUEST, RuleTaxYearRangeInvalidError),
            ("AA123456A", "XAIS12345678910", "2025-26", BAD_REQUEST, RuleTaxYearNotSupportedError)
          )

          input.foreach(validationErrorTest.tupled)
        }
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $downstreamStatus" in new Test {
            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              MtdIdLookupStub.ninoFound(nino)
              AuthStub.authorised()
              DownstreamStub.onError(DownstreamStub.DELETE, downstreamUri, downstreamStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request().delete())
            response.json shouldBe Json.toJson(expectedBody)
            response.status shouldBe expectedStatus
          }
        }

        val errors = List(
          (BAD_REQUEST, "1007", BAD_REQUEST, BusinessIdFormatError),
          (BAD_REQUEST, "1117", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "5010", NOT_FOUND, NotFoundError),
          (NOT_IMPLEMENTED, "5000", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "UNMATCHED_STUB_ERROR", BAD_REQUEST, RuleIncorrectGovTestScenarioError),
          (UNPROCESSABLE_ENTITY, "4200", BAD_REQUEST, RuleOutsideAmendmentWindow)
        )

        errors.foreach(serviceErrorTest.tupled)
      }
    }
  }

  private trait Test {
    val nino: String       = "AA123456A"
    val businessId: String = "X0IS12345678901"
    val taxYear: String    = "2026-27"

    def setupStubs(): StubMapping

    private def mtdUri: String = s"/$nino/businesses/$businessId/loss-claims/$taxYear"

    def request(): WSRequest = {
      AuthStub.resetAll()
      setupStubs()

      buildRequest(mtdUri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.7.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

    def downstreamUri: String = s"/itsd/reliefs/loss-claims/$nino/$businessId"
  }

}
