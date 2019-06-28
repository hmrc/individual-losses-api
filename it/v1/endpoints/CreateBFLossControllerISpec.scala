/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIED OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class CreateBFLossControllerISpec extends IntegrationBaseSpec {

  def objCreator(selfEmploymentId: Option[String], typeOfLoss: String, taxYear: String, lossAmount: BigDecimal): JsObject =
    Json.obj("selfEmploymentId" -> selfEmploymentId,
      "typeOfLoss" -> typeOfLoss,
      "taxYear" -> taxYear,
      "lossAmount" -> lossAmount)

  private trait Test {

    val nino = "AA123456A"
    val lossId = "AAZZ1234567890a"
    val correlationId = "X-123"
    val selfEmploymentId = "XKIS00000000988"
    val taxYear = "2019-20"
    val lossAmount = 256.78
    val typeOfLoss = "self-employment"

    val requestJson: JsValue = Json.parse(
      """
        |{
        |    "selfEmploymentId": "XKIS00000000988",
        |    "typeOfLoss": "self-employment",
        |    "taxYear": "2019-20",
        |    "lossAmount": 256.78
        |}
      """.stripMargin)

    val responseJson: JsValue = Json.parse(
      """
        |{
        |    "id": "AAZZ1234567890a"
        |}
      """.stripMargin)

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "des message"
         |      }
      """.stripMargin

    def setupStubs(): StubMapping

    def uri: String

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

  }

  "Calling the create BFLoss endpoint" should {

    trait CreateBFLossControllerTest extends Test {
      def uri: String = s"/individual/losses/$nino/brought-forward-losses"
    }

    "return a 201 status code" when {

      "any valid request is made" in new CreateBFLossControllerTest() {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.serviceSuccess(nino)
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe Status.CREATED
        response.json shouldBe responseJson

      }
    }


    "return a 404 status code" when {
      "any valid request is made but no income source has been found" in new CreateBFLossControllerTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.serviceError(nino, Status.NOT_FOUND, errorBody("NOT_FOUND_INCOME_SOURCE"))

        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe Status.NOT_FOUND
        response.json shouldBe Json.toJson(NotFoundError)
      }
    }

    def createErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
      s"des returns an $desCode error" in new CreateBFLossControllerTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.serviceError(nino, desStatus, errorBody(desCode))
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }

    "return 500 (Internal Server Error)" when {

      createErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 400 (Bad Request)" when {

      createBFLossValidationErrorTest("BADNINO", "NinoFormatError", Status.BAD_REQUEST, NinoFormatError)
      createBFLossValidationErrorTest("AA123456A", "TaxYearFormatError" , Status.BAD_REQUEST, TaxYearFormatError)
      createBFLossValidationErrorTest("AA123456A", "RuleIncorrectOrEmptyBodyError", Status.BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
      createBFLossValidationErrorTest("AA123456A", "RuleTaxYearNotSupportedError", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      createBFLossValidationErrorTest("AA123456A", "RuleTaxYearRangeExceededError", Status.BAD_REQUEST, RuleTaxYearRangeExceededError)
      createBFLossValidationErrorTest("AA123456A", "RuleTypeOfLossUnsupported", Status.BAD_REQUEST, RuleTypeOfLossUnsupported)
      createBFLossValidationErrorTest("AA123456A", "RuleInvalidSelfEmploymentId", Status.BAD_REQUEST, RuleInvalidSelfEmploymentId)
      createBFLossValidationErrorTest("AA123456A", "RulePropertySelfEmploymentId", Status.BAD_REQUEST, RulePropertySelfEmploymentId)
      createBFLossValidationErrorTest("AA123456A", "RuleInvalidLossAmount", Status.BAD_REQUEST, RuleInvalidLossAmount)
    }

    "return 404 NOT FOUND" when {
      createErrorTest(Status.NOT_FOUND, "NOT_FOUND_INCOME_SOURCE", Status.NOT_FOUND, NotFoundError)
    }

    def createBFLossValidationErrorTest(requestNino: String, bodyType: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
      s"validation fails with ${expectedBody.code} error" in new CreateBFLossControllerTest {

        override val nino: String = requestNino

        private val requestBody = bodyType match {
          case "NinoFormatError" => objCreator(Some(selfEmploymentId), typeOfLoss, taxYear, lossAmount)
          case "TaxYearFormatError" => objCreator(Some(selfEmploymentId), typeOfLoss, "20111", lossAmount)
          case "RuleIncorrectOrEmptyBodyError" => Json.toJson("dsdfs")
          case "RuleTaxYearNotSupportedError" => objCreator(Some(selfEmploymentId), typeOfLoss, "2011-12", lossAmount)
          case "RuleTaxYearRangeExceededError" => objCreator(Some(selfEmploymentId), typeOfLoss, "2019-25", lossAmount)
          case "RuleTypeOfLossUnsupported" => objCreator(None, "self-employment-class", "2019-20", lossAmount)
          case "RuleInvalidSelfEmploymentId" => objCreator(Some("sdfsf"), typeOfLoss, "2019-20", lossAmount)
          case "RulePropertySelfEmploymentId" => objCreator(Some("selfEmploymentId"), "uk-other-property", "2019-20", lossAmount)
          case "RuleInvalidLossAmount" => objCreator(Some(selfEmploymentId), typeOfLoss, taxYear, -3234)
        }
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().post(requestBody))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }
  }
}
