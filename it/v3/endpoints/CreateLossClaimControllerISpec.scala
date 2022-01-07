/*
 * Copyright 2022 HM Revenue & Customs
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

package v3.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.V3IntegrationBaseSpec
import v3.models.errors._
import v3.stubs.{AuditStub, AuthStub, IfsStub, MtdIdLookupStub}

class CreateLossClaimControllerISpec extends V3IntegrationBaseSpec {

  def generateLossClaim(businessId: String, typeOfLoss: String, taxYear: String, typeOfClaim: String): JsObject =
    Json.obj("businessId" -> businessId, "typeOfLoss" -> typeOfLoss, "taxYearClaimedFor" -> taxYear, "typeOfClaim" -> typeOfClaim)

  val businessId    = "XKIS00000000988"
  val taxYear       = "2019-20"
  val typeOfClaim   = "carry-forward"
  val typeOfLoss    = "self-employment"
  val claimId       = "AAZZ1234567890a"

  private trait Test {

    val nino = "AA123456A"

    val requestJson: JsValue = Json.parse("""
        |{
        |    "businessId": "XKIS00000000988",
        |    "typeOfLoss": "self-employment",
        |    "taxYearClaimedFor": "2019-20",
        |    "typeOfClaim": "carry-forward"
        |}
      """.stripMargin)

    val responseJson: JsValue = Json.parse(s"""
        |{
        |    "claimId": "AAZZ1234567890a",
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

    val downstreamResponseJson: JsValue = Json.parse("""
        |{
        |    "claimId": "AAZZ1234567890a"
        |}
      """.stripMargin)

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "downstream message"
         |      }
      """.stripMargin

    def setupStubs(): StubMapping

    def uri: String

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.3.0+json"))
    }

  }

  "Calling the create LossClaim endpoint" should {

    trait CreateLossClaimControllerTest extends Test {
      def uri: String    = s"/$nino/loss-claims"
      def ifsUrl: String = s"/income-tax/claims-for-relief/$nino"
    }

    "return a 201 status code" when {

      "any valid request is made" in new CreateLossClaimControllerTest() {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          IfsStub.onSuccess(IfsStub.POST, ifsUrl, Status.OK, downstreamResponseJson)
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe Status.CREATED
        response.json shouldBe responseJson
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return 500 (Internal Server Error)" when {
      createErrorTest(Status.BAD_REQUEST, "INVALID_CORRELATIONID", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 403 FORBIDDEN" when {
      createErrorTest(Status.CONFLICT, "DUPLICATE", Status.FORBIDDEN, RuleDuplicateClaimSubmissionError)
      createErrorTest(Status.UNPROCESSABLE_ENTITY, "ACCOUNTING_PERIOD_NOT_ENDED", Status.FORBIDDEN, RulePeriodNotEnded)
      createErrorTest(Status.UNPROCESSABLE_ENTITY, "NO_ACCOUNTING_PERIOD", Status.FORBIDDEN, RuleNoAccountingPeriod)
    }

    "return 404 NOT FOUND" when {
      createErrorTest(Status.NOT_FOUND, "INCOME_SOURCE_NOT_FOUND", Status.NOT_FOUND, NotFoundError)
    }

    "return 400 (Bad Request) with paths for the missing mandatory field" when {
      createLossClaimValidationErrorTest("AA123456A",
        Json.obj("typeOfLoss" -> typeOfLoss, "taxYearClaimedFor" -> taxYear, "typeOfClaim" -> typeOfClaim),
        Status.BAD_REQUEST,
        RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq("/businessId"))))
    }

    "return 400 (Bad Request)" when {

      createErrorTest(Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError)
      createErrorTest(Status.UNPROCESSABLE_ENTITY, "INVALID_CLAIM_TYPE", Status.BAD_REQUEST, RuleTypeOfClaimInvalid)
      createErrorTest(Status.UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      createLossClaimValidationErrorTest("BADNINO",
                                         generateLossClaim(businessId, typeOfLoss, taxYear, "carry-forward"),
                                         Status.BAD_REQUEST,
                                         NinoFormatError)
      createLossClaimValidationErrorTest("AA123456A",
                                         generateLossClaim(businessId, typeOfLoss, "20111", "carry-forward"),
                                         Status.BAD_REQUEST,
                                         TaxYearFormatError.copy(paths = Some(List("/taxYearClaimedFor"))))
      createLossClaimValidationErrorTest("AA123456A", Json.obj(), Status.BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
      createLossClaimValidationErrorTest("AA123456A",
                                         generateLossClaim(businessId, typeOfLoss, "2011-12", "carry-forward"),
                                         Status.BAD_REQUEST,
                                         RuleTaxYearNotSupportedError)
      createLossClaimValidationErrorTest(
        "AA123456A",
        generateLossClaim(businessId, typeOfLoss, "2019-25", "carry-forward"),
        Status.BAD_REQUEST,
        RuleTaxYearRangeInvalid.copy(paths = Some(List("/taxYearClaimedFor")))
      )
      createLossClaimValidationErrorTest("AA123456A",
                                         generateLossClaim(businessId, "self-employment-class", "2019-20", "carry-forward"),
                                         Status.BAD_REQUEST,
                                         TypeOfLossFormatError)
      createLossClaimValidationErrorTest("AA123456A",
                                         generateLossClaim("sdfsf", typeOfLoss, "2019-20", "carry-forward"),
                                         Status.BAD_REQUEST,
                                         BusinessIdFormatError)
      createLossClaimValidationErrorTest("AA123456A",
                                         generateLossClaim(businessId, typeOfLoss, taxYear, "carry-sideways-fhl"),
                                         Status.BAD_REQUEST,
                                         RuleTypeOfClaimInvalid)
      createLossClaimValidationErrorTest("AA123456A",
                                         generateLossClaim(businessId, typeOfLoss, taxYear, "carry-forward-type"),
                                         Status.BAD_REQUEST,
                                         TypeOfClaimFormatError)
    }

    def createErrorTest(ifsStatus: Int, ifsCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
      s"downstream returns an $ifsCode error" in new CreateLossClaimControllerTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          IfsStub.onError(IfsStub.POST, ifsUrl, ifsStatus, errorBody(ifsCode))
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
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
