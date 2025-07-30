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

package v5.lossClaim.amendOrder.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors.*
import play.api.libs.json.*
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*
import shared.models.errors.*
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec
import v5.lossClaims.amendOrder.def1.model.request.Claim
import v5.lossClaims.common.models.TypeOfClaim

class Def1_AmendLossClaimsOrderHipISpec extends IntegrationBaseSpec {

  val claim1: Claim        = Claim("1234567890ABEF1", 1)
  val claim2: Claim        = Claim("1234567890ABCDE", 2)
  val claim3: Claim        = Claim("1234567890ABDE0", 3)
  val claimSeq: Seq[Claim] = Seq(claim2, claim1, claim3)

  def requestJson(typeOfClaim: String = TypeOfClaim.`carry-sideways`.toString, listOfLossClaims: Seq[Claim] = claimSeq): JsValue = {
    // resetting custom writes for Seq[Claim] so it doesn't use custom Writes defined in the model
    def writes: OWrites[Claim]        = Json.writes[Claim]
    def writesSeq: Writes[Seq[Claim]] = Writes.seq[Claim](writes)
    Json.parse(
      s"""
        |{
        |   "typeOfClaim": "$typeOfClaim",
        |   "listOfLossClaims": ${Json.toJson(listOfLossClaims)(writesSeq)}
        |}
      """.stripMargin
    )
  }

  "Calling the Amend Loss Claims Order V5 endpoint" should {

    "return a 200 status code" when {

      "any valid request is made for a Tax Year Specific tax year" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, downstreamQueryParam, NO_CONTENT, JsObject.empty)
        }

        val response: WSResponse = await(request().put(requestJson()))
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: String,
                              requestBody: JsValue,
                              expectedStatus: Int,
                              expectedError: MtdError): Unit = {
        s"validation fails with ${expectedError.code} error" in new Test {

          override val nino: String    = requestNino
          override val taxYear: String = requestTaxYear

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
          }

          val response: WSResponse = await(request().put(requestBody))
          response.status shouldBe expectedStatus
          response.json shouldBe expectedError.asJson
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      validationErrorTest("BADNINO", "2019-20", requestJson(), BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", "BadDate", requestJson(), BAD_REQUEST, TaxYearClaimedForFormatError)
      validationErrorTest("AA123456A", "2020-22", requestJson(), BAD_REQUEST, RuleTaxYearRangeInvalidError)
      validationErrorTest("AA123456A", "2017-18", requestJson(), BAD_REQUEST, RuleTaxYearNotSupportedError)
      validationErrorTest("AA123456A", "2019-20", requestJson(typeOfClaim = "carry-sideways-fhl"), BAD_REQUEST, TypeOfClaimFormatError)
      validationErrorTest("AA123456A", "2023-24", JsObject.empty, BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
      validationErrorTest(
        "AA123456A",
        "2019-20",
        requestJson(listOfLossClaims = Seq(claim1.copy(claimId = "BadId"))),
        BAD_REQUEST,
        ClaimIdFormatError.withPath("/listOfLossClaims/0/claimId")
      )
      validationErrorTest(
        "AA123456A",
        "2019-20",
        requestJson(listOfLossClaims = Range(1, 101).map(Claim("1234567890ABEF1", _))),
        BAD_REQUEST,
        ValueFormatError.forPathAndRange("/listOfLossClaims/99/sequence", "1", "99")
      )
      validationErrorTest("AA123456A", "2019-20", requestJson(listOfLossClaims = Seq(claim2, claim3)), BAD_REQUEST, RuleInvalidSequenceStart)
      validationErrorTest("AA123456A", "2019-20", requestJson(listOfLossClaims = Seq(claim1, claim3)), BAD_REQUEST, RuleSequenceOrderBroken)
    }

    "handle downstream errors according to spec" when {
      def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedError: MtdError): Unit = {
        s"downstream returns $downstreamCode" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.PUT, downstreamUri, downstreamQueryParam, downstreamStatus, errorBody(downstreamCode))
          }

          val response: WSResponse = await(request().put(requestJson()))
          response.json shouldBe expectedError.asJson
          response.status shouldBe expectedStatus
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val errors = List(
        (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
        (BAD_REQUEST, "1117", BAD_REQUEST, TaxYearClaimedForFormatError),
        (BAD_REQUEST, "1216", INTERNAL_SERVER_ERROR, InternalError),
        (UNPROCESSABLE_ENTITY, "1000", INTERNAL_SERVER_ERROR, InternalError),
        (UNPROCESSABLE_ENTITY, "1108", NOT_FOUND, NotFoundError),
        (UNPROCESSABLE_ENTITY, "1109", BAD_REQUEST, RuleSequenceOrderBroken),
        (UNPROCESSABLE_ENTITY, "1110", BAD_REQUEST, RuleInvalidSequenceStart),
        (UNPROCESSABLE_ENTITY, "1111", BAD_REQUEST, RuleLossClaimsMissing),
        (NOT_IMPLEMENTED, "5000", BAD_REQUEST, RuleTaxYearNotSupportedError)
      )

      errors.foreach(args => (serviceErrorTest _).tupled(args))

    }

  }

  private trait Test {

    val nino: String                              = "AA123456A"
    val taxYear: String                           = "2023-24"
    val downstreamQueryParam: Map[String, String] = Map("taxYear" -> "23-24")
    val downstreamUri: String                     = s"/itsd/income-sources/claims-for-relief/$nino/preferences"

    private def uri: String = s"/$nino/loss-claims/order/$taxYear"

    def errorBody(code: String): String =
      s"""
        |[
        |  {
        |    "errorCode": "$code",
        |    "errorDescription": "error description"
        |  }
        |]
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

}
