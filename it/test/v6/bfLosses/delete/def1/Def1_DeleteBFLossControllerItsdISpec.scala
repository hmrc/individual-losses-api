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

package v6.bfLosses.delete.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.{LossIdFormatError, RuleDeleteAfterFinalDeclarationError, RuleOutsideAmendmentWindow}
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.domain.TaxYear
import shared.models.errors._
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_DeleteBFLossControllerItsdISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino    = "AA123456A"
    val lossId  = "AAZZ1234567890a"
    val taxYear = "2019-20"

    private def uri: String                   = s"/$nino/brought-forward-losses/$lossId/tax-year/$taxYear"
    def hipUrl: String                        = s"/itsd/income-sources/brought-forward-losses/$nino/$lossId"
    def downstreamParams: Map[String, String] = Map("taxYear" -> TaxYear.fromMtd(taxYear).asTysDownstream)

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

  "Calling the delete BFLoss endpoint" should {

    "return a 204 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.DELETE, hipUrl, downstreamParams, NO_CONTENT, JsObject.empty)
        }

        val response: WSResponse = await(request().delete())
        response.status shouldBe NO_CONTENT
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "handle errors according to spec" when {
      def serviceErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $desCode error" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.DELETE, hipUrl, downstreamParams, desStatus, errorBody(desCode))
          }

          val response: WSResponse = await(request().delete())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError)
      serviceErrorTest(BAD_REQUEST, "1219", BAD_REQUEST, LossIdFormatError)
      serviceErrorTest(CONFLICT, "1227", BAD_REQUEST, RuleDeleteAfterFinalDeclarationError)
      serviceErrorTest(UNPROCESSABLE_ENTITY, "4200", BAD_REQUEST, RuleOutsideAmendmentWindow)
      serviceErrorTest(NOT_IMPLEMENTED, "5000", BAD_REQUEST, RuleTaxYearNotSupportedError)
      serviceErrorTest(NOT_FOUND, "5010", NOT_FOUND, NotFoundError)
      serviceErrorTest(INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestLossId: String,
                              requestTaxYear: String,
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String    = requestNino
          override val lossId: String  = requestLossId
          override val taxYear: String = requestTaxYear

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

      validationErrorTest("BADNINO", "AAZZ1234567890a", "2019-20", BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", "BADLOSSID", "2019-20", BAD_REQUEST, LossIdFormatError)
      validationErrorTest("AA123456A", "AAZZ1234567890a", "BADTAXYEAR", BAD_REQUEST, TaxYearFormatError)
      validationErrorTest("AA123456A", "AAZZ1234567890a", "2020-22", BAD_REQUEST, RuleTaxYearRangeInvalidError)
      validationErrorTest("AA123456A", "AAZZ1234567890a", "2017-18", BAD_REQUEST, RuleTaxYearNotSupportedError)
    }

  }

}
