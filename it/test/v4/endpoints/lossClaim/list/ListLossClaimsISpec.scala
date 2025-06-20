/*
 * Copyright 2023 HM Revenue & Customs
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

package v4.endpoints.lossClaim.list

import common.errors.{TaxYearClaimedForFormatError, TypeOfClaimFormatError, TypeOfLossFormatError}
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.domain.TaxYear
import shared.models.errors._
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec
import v4.fixtures.ListLossClaimsFixtures._

class ListLossClaimsISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino                        = "AA123456A"
    val taxYear: String             = "2019-20"
    val typeOfLoss: Option[String]  = None
    val businessId: Option[String]  = None
    val typeOfClaim: Option[String] = None

    def mtdUrl: String = s"/$nino/loss-claims/tax-year/$taxYear"

    def downstreamUrl(taxYear: String = "2019-20"): String = s"/income-tax/${TaxYear.fromMtd(taxYear).asTysDownstream}/claims-for-relief/$nino"

    private def mtdQueryParams: Seq[(String, String)] =
      List("typeOfLoss" -> typeOfLoss, "businessId" -> businessId, "typeOfClaim" -> typeOfClaim)
        .collect { case (k, Some(v)) =>
          (k, v)
        }

    def errorBody(code: String): String =
      s"""
         |{
         |  "code": "$code",
         |  "reason": "downstream message"
         |}
      """.stripMargin

    def setupStubs(): Unit = {}

    def stubDownstream(response: JsValue, taxYear: String = "2019-20", params: Map[String, String] = Map.empty, status: Int = Status.OK): Unit = {
      DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl(taxYear), params, status, response)
    }

    def request(): WSRequest = {
      AuditStub.audit()
      AuthStub.authorised()
      MtdIdLookupStub.ninoFound(nino)
      setupStubs()

      buildRequest(mtdUrl)
        .addQueryStringParameters(mtdQueryParams: _*)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.4.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

  "Calling the ListLossClaims endpoint" should {
    "return a 200 status code" when {
      "query for everything with a tax year" in new Test {
        val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [${nonFhlClaimMtdJson(taxYear, nino)}],
             |    "links": ${baseHateoasLinks(taxYear, nino)}
             |}""".stripMargin)

        override def setupStubs(): Unit = {
          stubDownstream(nonFhlDownstreamResponseJson("2019-20"))
        }
        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }
      "querying for a specific typeOfLoss" in new Test {
        override val typeOfLoss: Option[String] = Some("uk-property-non-fhl")
        val downstreamResponse: JsValue         = nonFhlDownstreamResponseJson("2019-20")

        val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [${nonFhlClaimMtdJson(taxYear, nino)}],
             |    "links": ${baseHateoasLinks(taxYear, nino)}
             |}""".stripMargin)

        override def setupStubs(): Unit = {
          stubDownstream(downstreamResponse, params = Map("incomeSourceType" -> "02"))
        }
        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }
      "querying for a specific businessId" in new Test {
        override val businessId: Option[String] = Some("XAIS12345678911")
        val downstreamResponse: JsValue         = selfEmploymentDownstreamResponseJson("2019-20")

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |    "claims": [${selfEmploymentClaimMtdJson(taxYear, nino)}],
             |    "links": ${baseHateoasLinks(taxYear, nino)}
             |}
          """.stripMargin
        )

        override def setupStubs(): Unit = {
          stubDownstream(downstreamResponse, params = Map("incomeSourceId" -> "XAIS12345678911"))
        }
        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }
      "querying for specific typeOfClaim" in new Test {
        override val typeOfClaim: Option[String] = Some("carry-sideways")
        val downstreamResponse: JsValue          = selfEmploymentDownstreamResponseJson("2019-20")

        val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [${selfEmploymentClaimMtdJson(taxYear, nino)}],
             |    "links": ${baseHateoasLinks(taxYear, nino)}
             |}
       """.stripMargin)

        override def setupStubs(): Unit = {
          stubDownstream(downstreamResponse, params = Map("claimType" -> "CSGI"))
        }
        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }
    "return a 404 status code" when {
      "an empty array (no loss claims exists) is returned from backend" in new Test {
        override def setupStubs(): Unit = {
          stubDownstream(Json.parse("[]"))
        }
        val response: WSResponse = await(request().get())
        response.json shouldBe NotFoundError.asJson
        response.status shouldBe Status.NOT_FOUND
        response.header("Content-Type") shouldBe Some("application/json")
      }
      "the request is made without a tax year path parameter" in new Test {
        override def mtdUrl: String = s"/$nino/loss-claims"
        val response: WSResponse    = await(request().get())
        response.json shouldBe NotFoundError.asJson
        response.status shouldBe Status.NOT_FOUND
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }
    "return a 500 status code" when {
      "empty loss claims object inside the array is returned from backend" in new Test {
        override def setupStubs(): Unit = {
          stubDownstream(Json.parse("[{}]"))
        }
        val response: WSResponse = await(request().get())
        response.status shouldBe Status.INTERNAL_SERVER_ERROR
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }
    "handle downstream errors according to spec when given a tax year" when {
      def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $downstreamCode error" in new Test {
          override def setupStubs(): Unit = {
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl(), Map.empty, downstreamStatus, errorBody(downstreamCode))
          }
          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }
      val errors = List(
        (Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError),
        (Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError),
        (Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, InternalError),
        (Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, InternalError),
        (Status.BAD_REQUEST, "INVALID_CLAIM_TYPE", Status.BAD_REQUEST, TypeOfClaimFormatError),
        (Status.BAD_REQUEST, "INVALID_CORRELATION_ID", Status.INTERNAL_SERVER_ERROR, InternalError),
        (Status.BAD_REQUEST, "INVALID_TAX_YEAR", Status.BAD_REQUEST, TaxYearClaimedForFormatError),
        (Status.BAD_REQUEST, "INVALID_INCOMESOURCE_ID", Status.BAD_REQUEST, BusinessIdFormatError),
        (Status.BAD_REQUEST, "INVALID_INCOMESOURCE_TYPE", Status.BAD_REQUEST, TypeOfLossFormatError),
        (Status.BAD_REQUEST, "TAX_YEAR_NOT_SUPPORTED", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      )
      errors.foreach(args => (serviceErrorTest _).tupled(args))
    }
    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: String,
                              requestTypeOfLoss: Option[String],
                              requestSelfEmploymentId: Option[String],
                              requestTypeOfClaim: Option[String],
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {
          override val nino: String                = requestNino
          override val taxYear: String             = requestTaxYear
          override val typeOfLoss: Option[String]  = requestTypeOfLoss
          override val businessId: Option[String]  = requestSelfEmploymentId
          override val typeOfClaim: Option[String] = requestTypeOfClaim
          val response: WSResponse                 = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }
      val errors = List(
        ("AA1234", "2019-20", None, None, None, Status.BAD_REQUEST, NinoFormatError),
        ("AA123456A", "XXXX-YY", None, None, None, Status.BAD_REQUEST, TaxYearClaimedForFormatError),
        ("AA123456A", "2018-19", None, None, None, Status.BAD_REQUEST, RuleTaxYearNotSupportedError),
        ("AA123456A", "2019-21", None, None, None, Status.BAD_REQUEST, RuleTaxYearRangeInvalidError),
        ("AA123456A", "2019-20", Some("employment"), None, None, Status.BAD_REQUEST, TypeOfLossFormatError),
        ("AA123456A", "2019-20", Some("self-employment"), Some("XKIS0000000"), None, Status.BAD_REQUEST, BusinessIdFormatError),
        ("AA123456A", "2019-20", None, None, Some("FORWARD"), Status.BAD_REQUEST, TypeOfClaimFormatError)
      )
      errors.foreach(args => (validationErrorTest _).tupled(args))
    }

  }

}
