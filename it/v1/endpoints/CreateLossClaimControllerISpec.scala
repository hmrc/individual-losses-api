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
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class CreateLossClaimControllerISpec extends IntegrationBaseSpec {

  def generateLossClaim(selfEmploymentId: Option[String], typeOfLoss: String, taxYear: String, typeOfClaim: String): JsObject =
    Json.obj("selfEmploymentId" -> selfEmploymentId,
      "typeOfLoss" -> typeOfLoss,
      "taxYear" -> taxYear,
      "typeOfClaim" -> typeOfClaim)

  val correlationId = "X-123"
  val selfEmploymentId = "XKIS00000000988"
  val taxYear = "2019-20"
  val typeOfClaim = "carry-forward"
  val typeOfLoss = "self-employment"
  val claimId = "AAZZ1234567890a"

  private trait Test {

    val nino = "AA123456A"

    val requestJson: JsValue = Json.parse(
      """
        |{
        |    "selfEmploymentId": "XKIS00000000988",
        |    "typeOfLoss": "self-employment",
        |    "taxYear": "2019-20",
        |    "typeOfClaim": "carry-forward"
        |}
      """.stripMargin)

    val responseJson: JsValue = Json.parse(
      s"""
        |{
        |    "id": "AAZZ1234567890a",
        |    "links": [{
        |      "href": "/individuals/losses/$nino/loss-claims/$claimId",
        |      "method": "GET",
        |      "rel": "self"
        |    },
        |    {
        |      "href": "/individuals/losses/$nino/loss-claims/$claimId",
        |      "method": "DELETE",
        |      "rel": "delete-loss-claim"
        |    },{
        |      "href": "/individuals/losses/$nino/loss-claims/$claimId/change-type-of-claim",
        |      "method": "POST",
        |      "rel": "amend-loss-claim"
        |    }
        |    ]
        |}
      """.stripMargin)

    val desResponseJson: JsValue = Json.parse(
      """
        |{
        |    "claimId": "AAZZ1234567890a"
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

  "Calling the create LossClaim endpoint" should {

    trait CreateLossClaimControllerTest extends Test {
      def uri: String = s"/$nino/loss-claims"
      def desUrl: String = s"/income-tax/claims-for-relief/$nino"
    }

    "return a 201 status code" when {

      "any valid request is made" in new CreateLossClaimControllerTest() {

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
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return 500 (Internal Server Error)" when {

      createErrorTest(Status.BAD_REQUEST, "UNEXPECTED_DES_ERROR_CODE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)

      // These use default mapping but in spec so check explicity...
      createErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.FORBIDDEN, "INVALID_TAX_YEAR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.FORBIDDEN, "INCOME_SOURCE_NOT_ACTIVE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.FORBIDDEN, "TAX_YEAR_NOT_ENDED", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 400 (Bad Request)" when {

      createErrorTest(Status.FORBIDDEN, "INVALID_CLAIM_TYPE", Status.BAD_REQUEST, RuleTypeOfClaimInvalid)
      createErrorTest(Status.FORBIDDEN, "TAX_YEAR_NOT_SUPPORTED", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
    }

    "return 403 FORBIDDEN" when {
      createErrorTest(Status.CONFLICT, "DUPLICATE", Status.FORBIDDEN, RuleDuplicateClaimSubmissionError)
      createErrorTest(Status.FORBIDDEN, "ACCOUNTING_PERIOD_NOT_ENDED", Status.FORBIDDEN, RulePeriodNotEnded)
      createErrorTest(Status.FORBIDDEN, "NO_ACCOUNTING_PERIOD", Status.FORBIDDEN, RuleNoAccountingPeriod)
    }

    "return 404 NOT FOUND" when {
      createErrorTest(Status.NOT_FOUND, "NOT_FOUND_INCOME_SOURCE", Status.NOT_FOUND, NotFoundError)
    }

    def createErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
      s"des returns an $desCode error" in new CreateLossClaimControllerTest {

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

      createLossClaimValidationErrorTest("BADNINO", generateLossClaim(Some(selfEmploymentId), typeOfLoss, taxYear,
        "carry-forward"), Status.BAD_REQUEST, NinoFormatError)
      createLossClaimValidationErrorTest("AA123456A",
        generateLossClaim(Some(selfEmploymentId), typeOfLoss, "20111", "carry-forward") , Status.BAD_REQUEST, TaxYearFormatError)
      createLossClaimValidationErrorTest("AA123456A", Json.toJson("dsdfs"), Status.BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
      createLossClaimValidationErrorTest("AA123456A",
        generateLossClaim(Some(selfEmploymentId), typeOfLoss, "2011-12", "carry-forward"), Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      createLossClaimValidationErrorTest("AA123456A",
        generateLossClaim(Some(selfEmploymentId), typeOfLoss, "2019-25", "carry-forward"), Status.BAD_REQUEST, RuleTaxYearRangeInvalid)
      createLossClaimValidationErrorTest("AA123456A",
        generateLossClaim(None, "self-employment-class", "2019-20", "carry-forward"), Status.BAD_REQUEST, TypeOfLossFormatError)
      createLossClaimValidationErrorTest("AA123456A",
        generateLossClaim(Some("sdfsf"), typeOfLoss, "2019-20", "carry-forward"), Status.BAD_REQUEST, SelfEmploymentIdFormatError)
      createLossClaimValidationErrorTest("AA123456A",
        generateLossClaim(Some("selfEmploymentId"), "uk-property-non-fhl", "2019-20", "carry-forward"), Status.BAD_REQUEST, RuleSelfEmploymentId)
      createLossClaimValidationErrorTest("AA123456A",
        generateLossClaim(Some(selfEmploymentId), typeOfLoss, taxYear,"carry-sideways-fhl"), Status.BAD_REQUEST, RuleTypeOfClaimInvalid)
      createLossClaimValidationErrorTest("AA123456A",
        generateLossClaim(Some(selfEmploymentId), typeOfLoss, taxYear,"carry-forward-type"), Status.BAD_REQUEST, TypeOfClaimFormatError)
    }


    def createLossClaimValidationErrorTest(requestNino: String, requestBody: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
      s"validation fails with ${expectedBody.code} error" in new CreateLossClaimControllerTest {

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
