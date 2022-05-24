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

package api.endpoints.lossClaim.amendOrder.v3

import api.endpoints.lossClaim.amendOrder.v3.model.Claim
import api.endpoints.lossClaim.domain.v3.TypeOfClaim
import api.models.domain.DownstreamTaxYear
import api.models.errors._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsValue, Json, OWrites, Writes}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import support.V3IntegrationBaseSpec
import support.stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import v3.models.errors._

class AmendLossClaimsOrderControllerISpec extends V3IntegrationBaseSpec {

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

  private trait Test {

    val nino: String    = "AA123456A"
    val taxYear: String = "2019-20"

    val responseJson: JsValue = Json.parse(
      s"""
        |{
        |  "links": [
        |    {
        |      "href": "/individuals/losses/$nino/loss-claims/order",
        |      "method": "PUT",
        |      "rel": "amend-loss-claim-order"
        |    },
        |    {
        |      "href": "/individuals/losses/$nino/loss-claims",
        |      "method": "GET",
        |      "rel": "self"
        |    }
        |  ]
        |}
      """.stripMargin
    )

    def uri: String    = s"/$nino/loss-claims/order/$taxYear"
    def ifsUrl: String = s"/income-tax/claims-for-relief/$nino/preferences/${DownstreamTaxYear.fromMtd(taxYear)}"

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
          (ACCEPT, "application/vnd.hmrc.3.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }
  }

  "Calling the Amend Loss Claims Order V3 endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, ifsUrl, Status.NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestJson()))
        response.status shouldBe Status.OK
        response.json shouldBe responseJson
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "handle downstream errors according to spec" when {
      def serviceErrorTest(ifsStatus: Int, ifsCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $ifsCode error" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.PUT, ifsUrl, ifsStatus, errorBody(ifsCode))
          }

          val response: WSResponse = await(request().withQueryStringParameters("taxYear" -> taxYear).put(requestJson()))
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_TAXYEAR", Status.BAD_REQUEST, TaxYearFormatError)
      serviceErrorTest(Status.CONFLICT, "CONFLICT_SEQUENCE_START", Status.BAD_REQUEST, RuleInvalidSequenceStart)
      serviceErrorTest(Status.CONFLICT, "CONFLICT_NOT_SEQUENTIAL", Status.BAD_REQUEST, RuleSequenceOrderBroken)
      serviceErrorTest(Status.CONFLICT, "CONFLICT_NOT_FULL_LIST", Status.BAD_REQUEST, RuleLossClaimsMissing)
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.INTERNAL_SERVER_ERROR, StandardDownstreamError)
      serviceErrorTest(Status.UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY", Status.NOT_FOUND, NotFoundError)
      serviceErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, StandardDownstreamError)
      serviceErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, StandardDownstreamError)
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: String,
                              requestBody: JsValue,
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String    = requestNino
          override val taxYear: String = requestTaxYear

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
          }

          val response: WSResponse = await(request().withQueryStringParameters("taxYear" -> taxYear).put(requestBody))
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      validationErrorTest("BADNINO", "2019-20", requestJson(), Status.BAD_REQUEST, NinoFormatError)
      validationErrorTest("AA123456A", "BadDate", requestJson(), Status.BAD_REQUEST, TaxYearFormatError)
      validationErrorTest("AA123456A", "2020-22", requestJson(), Status.BAD_REQUEST, RuleTaxYearRangeInvalid)
      validationErrorTest("AA123456A", "2017-18", requestJson(), Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      validationErrorTest("AA123456A", "2019-20", requestJson(typeOfClaim = "carry-sideways-fhl"), Status.BAD_REQUEST, TypeOfClaimFormatError)
      validationErrorTest("AA123456A", "2019-20", Json.obj(), Status.BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
      validationErrorTest(
        "AA123456A",
        "2019-20",
        requestJson(listOfLossClaims = Seq(claim1.copy(claimId = "BadId"))),
        Status.BAD_REQUEST,
        ClaimIdFormatError.copy(paths = Some(Seq("/listOfLossClaims/0/claimId")))
      )
      validationErrorTest(
        "AA123456A",
        "2019-20",
        requestJson(listOfLossClaims = Range(1, 101).map(Claim("1234567890ABEF1", _))),
        Status.BAD_REQUEST,
        ValueFormatError.forPathAndRange("/listOfLossClaims/99/sequence", "1", "99")
      )
      validationErrorTest("AA123456A", "2019-20", requestJson(listOfLossClaims = Seq(claim2, claim3)), Status.BAD_REQUEST, RuleInvalidSequenceStart)
      validationErrorTest("AA123456A", "2019-20", requestJson(listOfLossClaims = Seq(claim1, claim3)), Status.BAD_REQUEST, RuleSequenceOrderBroken)
    }
  }
}
