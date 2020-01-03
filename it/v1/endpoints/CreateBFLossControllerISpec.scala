/*
 * Copyright 2020 HM Revenue & Customs
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
import v1.hateoas.HateoasLinks
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class CreateBFLossControllerISpec extends IntegrationBaseSpec {

  def generateBFLoss(selfEmploymentId: Option[String], typeOfLoss: String, taxYear: String, lossAmount: BigDecimal): JsObject =
    Json.obj("selfEmploymentId" -> selfEmploymentId,
      "typeOfLoss" -> typeOfLoss,
      "taxYear" -> taxYear,
      "lossAmount" -> lossAmount)

  val lossId = "AAZZ1234567890a"
  val correlationId = "X-123"
  val selfEmploymentId = "XKIS00000000988"
  val taxYear = "2019-20"
  val lossAmount = 256.78
  val typeOfLoss = "self-employment"

  object Hateoas extends HateoasLinks

  private trait Test {

    val nino = "AA123456A"

    val requestJson: JsValue = Json.parse(
      """
        |{
        |    "selfEmploymentId": "XKIS00000000988",
        |    "typeOfLoss": "self-employment",
        |    "taxYear": "2019-20",
        |    "lossAmount": 256.78
        |}
      """.stripMargin)

    lazy val responseJson: JsValue = Json.parse(
      s"""
        |{
        |    "id": "AAZZ1234567890a",
        |    "links": [{
        |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
        |      "method": "GET",
        |      "rel": "self"
        |    },
        |    {
        |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
        |      "method": "DELETE",
        |      "rel": "delete-brought-forward-loss"
        |    },{
        |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId/change-loss-amount",
        |      "method": "POST",
        |      "rel": "amend-brought-forward-loss"
        |    }
        |    ]
        |}
      """.stripMargin)

    val desResponseJson: JsValue = Json.parse(
      """
        |{
        |    "lossId": "AAZZ1234567890a"
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
      def uri: String = s"/$nino/brought-forward-losses"
      def desUrl: String = s"/income-tax/brought-forward-losses/$nino"
    }

    "return a 201 status code" when {

      "any valid request is made" in new CreateBFLossControllerTest() {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.POST, desUrl, Status.OK, desResponseJson)
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe Status.CREATED
        response.json shouldBe responseJson
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }


    "return 500 (Internal Server Error)" when {

      createErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.BAD_REQUEST, "UNEXPECTED_DES_ERROR_CODE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 403 FORBIDDEN" when {
      createErrorTest(Status.CONFLICT, "DUPLICATE", Status.FORBIDDEN, RuleDuplicateSubmissionError)
    }

    "return 404 NOT FOUND" when {
      createErrorTest(Status.NOT_FOUND, "NOT_FOUND_INCOME_SOURCE", Status.NOT_FOUND, NotFoundError)
    }

    "return 400 BAD REQUEST" when {
      createErrorTest(Status.FORBIDDEN, "TAX_YEAR_NOT_SUPPORTED", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      createErrorTest(Status.FORBIDDEN, "TAX_YEAR_NOT_ENDED", Status.BAD_REQUEST, RuleTaxYearNotEndedError)
    }

    def createErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
      s"des returns an $desCode error" in new CreateBFLossControllerTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onError(DesStub.POST, desUrl, desStatus, errorBody(desCode))
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return 400 (Bad Request)" when {

      createBFLossValidationErrorTest("BADNINO", generateBFLoss(Some(selfEmploymentId), typeOfLoss, taxYear, lossAmount), Status.BAD_REQUEST, NinoFormatError)
      createBFLossValidationErrorTest("AA123456A",
        generateBFLoss(Some(selfEmploymentId), typeOfLoss, "20111", lossAmount) , Status.BAD_REQUEST, TaxYearFormatError)
      createBFLossValidationErrorTest("AA123456A", Json.toJson("dsdfs"), Status.BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
      createBFLossValidationErrorTest("AA123456A",
        generateBFLoss(Some(selfEmploymentId), typeOfLoss, "2011-12", lossAmount), Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      createBFLossValidationErrorTest("AA123456A",
        generateBFLoss(Some(selfEmploymentId), typeOfLoss, "2019-25", lossAmount), Status.BAD_REQUEST, RuleTaxYearRangeInvalid)
      createBFLossValidationErrorTest("AA123456A",
        generateBFLoss(None, "self-employment-class", "2019-20", lossAmount), Status.BAD_REQUEST, TypeOfLossFormatError)
      createBFLossValidationErrorTest("AA123456A",
        generateBFLoss(Some("sdfsf"), typeOfLoss, "2019-20", lossAmount), Status.BAD_REQUEST, SelfEmploymentIdFormatError)
      createBFLossValidationErrorTest("AA123456A",
        generateBFLoss(Some("selfEmploymentId"), "uk-property-non-fhl", "2019-20", lossAmount), Status.BAD_REQUEST, RuleSelfEmploymentId)
      createBFLossValidationErrorTest("AA123456A",
        generateBFLoss(Some(selfEmploymentId), typeOfLoss, taxYear,-3234.99), Status.BAD_REQUEST, RuleInvalidLossAmount)
      createBFLossValidationErrorTest("AA123456A",
        generateBFLoss(Some(selfEmploymentId), typeOfLoss, taxYear,99999999999.999), Status.BAD_REQUEST, AmountFormatError)
    }


    def createBFLossValidationErrorTest(requestNino: String, requestBody: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
      s"validation fails with ${expectedBody.code} error" in new CreateBFLossControllerTest {

        override val nino: String = requestNino
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(requestNino)
        }

        val response: WSResponse = await(request().post(requestBody))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }
  }
}
