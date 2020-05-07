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
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import play.api.http.HeaderNames.ACCEPT
import v1.models.domain.{Claim, TypeOfClaim}
import v1.models.errors._
import v1.models.requestData.DesTaxYear
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class AmendLossClaimsOrderControllerISpec extends IntegrationBaseSpec {

  val correlationId = "X-123"

  val claim1 = Claim("1234567890ABEF1", 1)
  val claim2 = Claim("1234567890ABCDE", 2)
  val claim3 = Claim("1234567890ABDE0", 3)
  val claimSeq = Seq(claim2, claim1, claim3)

  def requestJson(claimType: String = TypeOfClaim.`carry-sideways`.toString, listOfLossClaims: Seq[Claim] = claimSeq): JsValue = Json.parse(s"""
       |{
       |   "claimType": "$claimType",
       |   "listOfLossClaims": ${Json.toJson(listOfLossClaims)}
       |}
      """.stripMargin)

  private trait Test {

    val nino    = "AA123456A"
    val taxYear = "2019-20"

    val responseJson: JsValue =
      Json.parse(s"""
        |{
        |  "links": [
        |    {
        |      "href": "/individuals/losses/$nino/loss-claims/order",
        |      "method": "PUT",
        |      "rel": "self"
        |    },
        |    {
        |      "href": "/individuals/losses/$nino/loss-claims",
        |      "method": "GET",
        |      "rel": "list-loss-claims"
        |    }
        |  ]
        |}""".stripMargin)

    def uri: String    = s"/$nino/loss-claims/order"
    def desUrl: String = s"/income-tax/claims-for-relief/$nino/preferences/${DesTaxYear.fromMtd(taxYear)}"

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "des message"
         |      }
      """.stripMargin

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

  }

  "Calling the Amend Loss Claims Order endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.PUT, desUrl, Status.NO_CONTENT)
        }

        val response: WSResponse = await(request().withQueryStringParameters("taxYear" -> taxYear).put(requestJson()))
        response.status shouldBe Status.OK
        response.json shouldBe responseJson
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "any valid request is made with no taxYear" in new Test {

        override def desUrl: String = s"/income-tax/claims-for-relief/$nino/preferences/${DesTaxYear.mostRecentTaxYear()}"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.PUT, desUrl, Status.NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestJson()))
        response.status shouldBe Status.OK
        response.json shouldBe responseJson
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "handle downstream errors according to spec" when {
      def serviceErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"des returns an $desCode error" in new Test {

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DesStub.onError(DesStub.PUT, desUrl, desStatus, errorBody(desCode))
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
      serviceErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      serviceErrorTest(Status.UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY", Status.NOT_FOUND, NotFoundError)
      serviceErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      serviceErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
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
      validationErrorTest("AA123456A", "2019-20", requestJson(claimType = "carry-sideways-fhl"), Status.BAD_REQUEST, ClaimTypeFormatError)
      validationErrorTest("AA123456A", "2019-20", Json.toJson("""tfywfgef"""), Status.BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
      validationErrorTest("AA123456A", "2019-20", requestJson(listOfLossClaims = Seq(claim1.copy(id = "BadId"))), Status.BAD_REQUEST, ClaimIdFormatError)
      validationErrorTest("AA123456A", "2019-20", requestJson(listOfLossClaims = Range(1, 101).map(Claim("1234567890ABEF1", _))), Status.BAD_REQUEST, SequenceFormatError)
      validationErrorTest("AA123456A", "2019-20", requestJson(listOfLossClaims = Seq(claim2, claim3)), Status.BAD_REQUEST, RuleInvalidSequenceStart)
      validationErrorTest("AA123456A", "2019-20", requestJson(listOfLossClaims = Seq(claim1, claim3)), Status.BAD_REQUEST, RuleSequenceOrderBroken)
    }
  }
}
