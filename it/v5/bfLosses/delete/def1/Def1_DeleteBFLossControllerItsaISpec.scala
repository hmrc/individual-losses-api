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

package v5.bfLosses.delete.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.{LossIdFormatError, RuleDeleteAfterFinalDeclarationError}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers._
import shared.models.domain.TaxYear.currentTaxYear
import shared.models.errors._
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_DeleteBFLossControllerItsaISpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, Any] =
    Map("feature-switch.hipItsa_hipItsd_migration_1504.enabled" -> false) ++ super.servicesConfig

  private trait Test {

    val nino   = "AA123456A"
    val lossId = "AAZZ1234567890a"

    private def uri: String   = s"/$nino/brought-forward-losses/$lossId"
    def downstreamUrl: String = s"/itsa/income-tax/v1/brought-forward-losses/$nino/${currentTaxYear.asTysDownstream}/$lossId"

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

  "Calling the delete BFLoss endpoint" should {

    "return a 204 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.DELETE, downstreamUrl, NO_CONTENT, JsObject.empty)
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
            DownstreamStub.onError(DownstreamStub.DELETE, downstreamUrl, desStatus, errorBody(desCode))
          }

          val response: WSResponse = await(request().delete())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError)
      serviceErrorTest(BAD_REQUEST, "INVALID_LOSS_ID", BAD_REQUEST, LossIdFormatError)
      serviceErrorTest(BAD_REQUEST, "UNEXPECTED_DES_ERROR_CODE", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError)
      serviceErrorTest(CONFLICT, "CONFLICT", BAD_REQUEST, RuleDeleteAfterFinalDeclarationError)
      serviceErrorTest(INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
      serviceErrorTest(SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
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

          val response: WSResponse = await(request().delete())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      validationErrorTest("BADNINO", "AAZZ1234567890a", BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", "BADLOSSID", BAD_REQUEST, LossIdFormatError)
    }

  }

}
