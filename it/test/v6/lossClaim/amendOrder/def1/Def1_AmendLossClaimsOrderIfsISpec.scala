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

package v6.lossClaim.amendOrder.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.errors._
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json._
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors._
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec
import v6.lossClaims.amendOrder.def1.model.request.Claim
import v6.lossClaims.common.models.TypeOfClaim

class Def1_AmendLossClaimsOrderIfsISpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, Any] =
    Map("feature-switch.ifs_hip_migration_1793.enabled" -> false) ++ super.servicesConfig

  val claim1: Claim        = Claim("1234567890ABEF1", 1)
  val claim2: Claim        = Claim("1234567890ABCDE", 2)
  val claim3: Claim        = Claim("1234567890ABDE0", 3)
  val claimSeq: Seq[Claim] = Seq(claim2, claim1, claim3)

  def requestJson(typeOfClaim: String = TypeOfClaim.`carry-sideways`.toString, listOfLossClaims: Seq[Claim] = claimSeq): JsValue = {
    // resetting custom writes for Seq[Claim] so it doesn't use custom Writes defined in the model
    def writes: OWrites[Claim]        = Json.writes[Claim]
    def writesSeq: Writes[Seq[Claim]] = Writes.seq[Claim](writes)
    Json.parse(s"""
                  |{
                  |   "typeOfClaim": "$typeOfClaim",
                  |   "listOfLossClaims": ${Json.toJson(listOfLossClaims)(writesSeq)}
                  |}
      """.stripMargin)
  }

  "Calling the Amend Loss Claims Order endpoint" should {

    "return a 200 status code" when {

      "any valid request is made for a Tax Year Specific tax year" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, Status.NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestJson()))
        response.status shouldBe Status.OK
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

          val response: WSResponse = await(request().withQueryStringParameters("taxYear" -> taxYear).put(requestBody))
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
            DownstreamStub.onError(DownstreamStub.PUT, downstreamUri, downstreamStatus, errorBody(downstreamCode))
          }

          val response: WSResponse = await(request().withQueryStringParameters("taxYear" -> taxYear).put(requestJson()))
          response.json shouldBe expectedError.asJson
          response.status shouldBe expectedStatus
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val errors = List(
        (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
        (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearClaimedForFormatError),
        (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError),
        (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError),
        (CONFLICT, "NOT_SEQUENTIAL", BAD_REQUEST, RuleSequenceOrderBroken),
        (CONFLICT, "SEQUENCE_START", BAD_REQUEST, RuleInvalidSequenceStart),
        (CONFLICT, "NO_FULL_LIST", BAD_REQUEST, RuleLossClaimsMissing),
        (NOT_FOUND, "CLAIM_NOT_FOUND", NOT_FOUND, NotFoundError),
        (UNPROCESSABLE_ENTITY, "OUTSIDE_AMENDMENT_WINDOW", BAD_REQUEST, RuleOutsideAmendmentWindow),
        (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError),
        (Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
        (Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
      )

      errors.foreach(args => (serviceErrorTest _).tupled(args))

    }

  }

  private trait Test {

    val nino              = "AA123456A"
    val taxYear           = "2023-24"
    val downstreamTaxYear = "23-24"
    val downstreamUri     = s"/income-tax/claims-for-relief/preferences/$downstreamTaxYear/$nino"

    def uri: String = s"/$nino/loss-claims/order/$taxYear"

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
          (ACCEPT, "application/vnd.hmrc.6.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

}
