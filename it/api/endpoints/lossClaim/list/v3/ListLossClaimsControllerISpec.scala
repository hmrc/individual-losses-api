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

package api.endpoints.lossClaim.list.v3

import api.fixtures.v3.ListLossClaimsFixtures._
import api.models.domain.TaxYear
import api.models.errors._
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.test.Helpers.AUTHORIZATION
import support.V3V4IntegrationBaseSpec
import support.stubs.{ AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub }

class ListLossClaimsControllerISpec extends V3V4IntegrationBaseSpec {

  private trait Test {

    val nino                        = "AA123456A"
    val taxYear: Option[String]     = Some("2019-20")
    val typeOfLoss: Option[String]  = None
    val businessId: Option[String]  = None
    val typeOfClaim: Option[String] = None

    def mtdUrl: String = s"/$nino/loss-claims"

    def downstreamUrl(taxYear: String = "2019-20"): String = s"/income-tax/claims-for-relief/${TaxYear.fromMtd(taxYear).asTysDownstream}/$nino"

    def mtdQueryParams: Seq[(String, String)] =
      List("taxYearClaimedFor" -> taxYear, "typeOfLoss" -> typeOfLoss, "businessId" -> businessId, "typeOfClaim" -> typeOfClaim)
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

    def stubDownstream(response: JsValue, taxYear: String = "2019-20", params: Map[String, String] = Map.empty): Unit = {
      DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl(taxYear), params, OK, response)
    }

    def request(): WSRequest = {
      AuditStub.audit()
      AuthStub.authorised()
      MtdIdLookupStub.ninoFound(nino)
      setupStubs()

      buildRequest(mtdUrl)
        .addQueryStringParameters(mtdQueryParams: _*)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.3.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

  }

  "Calling the ListLossClaims endpoint" should {

    "return a 200 status code" when {

      "query for everything with a tax year" in new Test {
        val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [${nonFhlClaimMtdJson("2019-20", nino)}],
             |    "links": ${baseHateoasLinks(nino)}
             |}""".stripMargin)

        override def setupStubs(): Unit = {
          stubDownstream(nonFhlDownstreamResponseJson("2019-20"))
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "query for everything where a tax year is not given" in new Test {
        override val taxYear: Option[String] = None

        val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [
             |        ${nonFhlClaimMtdJson("2019-20", nino)},
             |        ${nonFhlClaimMtdJson("2020-21", nino)},
             |        ${nonFhlClaimMtdJson("2021-22", nino)},
             |        ${nonFhlClaimMtdJson("2022-23", nino)}
             |    ],
             |    "links": ${baseHateoasLinks(nino)}
             |}""".stripMargin)

        override def setupStubs(): Unit = {
          stubDownstream(nonFhlDownstreamResponseJson("2019-20"), "2019-20")
          stubDownstream(nonFhlDownstreamResponseJson("2020-21"), "2020-21")
          stubDownstream(nonFhlDownstreamResponseJson("2021-22"), "2021-22")
          stubDownstream(nonFhlDownstreamResponseJson("2022-23"), "2022-23")
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "query where a tax year is not given and some responses are 404 NOT_FOUND" in new Test {
        override val taxYear: Option[String] = None

        val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [
             |        ${nonFhlClaimMtdJson("2019-20", nino)},
             |        ${nonFhlClaimMtdJson("2021-22", nino)}
             |    ],
             |    "links": ${baseHateoasLinks(nino)}
             |}""".stripMargin)

        override def setupStubs(): Unit = {
          stubDownstream(nonFhlDownstreamResponseJson("2019-20"), "2019-20")
          DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2020-21"), Map.empty, NOT_FOUND, errorBody("NOT_FOUND"))
          stubDownstream(nonFhlDownstreamResponseJson("2021-22"), "2021-22")
          DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2022-23"), Map.empty, NOT_FOUND, errorBody("NOT_FOUND"))
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying for a specific typeOfLoss" in new Test {
        override val typeOfLoss: Option[String] = Some("uk-property-non-fhl")

        val downstreamResponse: JsValue = nonFhlDownstreamResponseJson("2019-20")

        val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [${nonFhlClaimMtdJson("2019-20", nino)}],
             |    "links": ${baseHateoasLinks(nino)}
             |}""".stripMargin)

        override def setupStubs(): Unit = {
          stubDownstream(downstreamResponse, params = Map("incomeSourceType" -> "02"))
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying for a specific businessId" in new Test {
        override val businessId: Option[String] = Some("XAIS12345678911")

        val downstreamResponse: JsValue = selfEmploymentDownstreamResponseJson("2019-20")

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |    "claims": [${selfEmploymentClaimMtdJson("2019-20", nino)}],
             |    "links": ${baseHateoasLinks(nino)}
             |}
          """.stripMargin
        )

        override def setupStubs(): Unit = {
          stubDownstream(downstreamResponse, params = Map("incomeSourceId" -> "XAIS12345678911"))
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying for specific typeOfClaim" in new Test {
        override val typeOfClaim: Option[String] = Some("carry-sideways")

        val downstreamResponse: JsValue = selfEmploymentDownstreamResponseJson("2019-20")

        val responseJson: JsValue = Json.parse(s"""
             |{
             |    "claims": [${selfEmploymentClaimMtdJson("2019-20", nino)}],
             |    "links": ${baseHateoasLinks(nino)}
             |}
       """.stripMargin)

        override def setupStubs(): Unit = {
          stubDownstream(downstreamResponse, params = Map("claimType" -> "CSGI"))
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
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
        response.status shouldBe NOT_FOUND
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return a 500 status code" when {
      "empty loss claims object inside the array is returned from backend" in new Test {
        override def setupStubs(): Unit = {
          stubDownstream(Json.parse("[{}]"))
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe INTERNAL_SERVER_ERROR
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
        (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
        (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError),
        (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
        (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_CLAIM_TYPE", BAD_REQUEST, TypeOfClaimFormatError),
        (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
        (BAD_REQUEST, "INVALID_INCOMESOURCE_ID", BAD_REQUEST, BusinessIdFormatError),
        (BAD_REQUEST, "INVALID_INCOMESOURCE_TYPE", BAD_REQUEST, TypeOfLossFormatError),
        (BAD_REQUEST, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
      )

      errors.foreach(args => (serviceErrorTest _).tupled(args))
    }

    "handle downstream errors according to spec without a tax year" when {
      def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $downstreamCode error" in new Test {
          override val taxYear: Option[String] = None

          override def setupStubs(): Unit = {
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2019-20"), Map.empty, downstreamStatus, errorBody(downstreamCode))
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2020-21"), Map.empty, downstreamStatus, errorBody(downstreamCode))
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2021-22"), Map.empty, downstreamStatus, errorBody(downstreamCode))
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2022-23"), Map.empty, downstreamStatus, errorBody(downstreamCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val errors = List(
        (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
        (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError),
        (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
        (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_CLAIM_TYPE", BAD_REQUEST, TypeOfClaimFormatError),
        (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_TAX_YEAR", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_INCOMESOURCE_ID", BAD_REQUEST, BusinessIdFormatError),
        (BAD_REQUEST, "INVALID_INCOMESOURCE_TYPE", BAD_REQUEST, TypeOfLossFormatError),
        (BAD_REQUEST, "TAX_YEAR_NOT_SUPPORTED", INTERNAL_SERVER_ERROR, InternalError)
      )

      errors.foreach(args => (serviceErrorTest _).tupled(args))
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: Option[String],
                              requestTypeOfLoss: Option[String],
                              requestSelfEmploymentId: Option[String],
                              requestTypeOfClaim: Option[String],
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String                = requestNino
          override val taxYear: Option[String]     = requestTaxYear
          override val typeOfLoss: Option[String]  = requestTypeOfLoss
          override val businessId: Option[String]  = requestSelfEmploymentId
          override val typeOfClaim: Option[String] = requestTypeOfClaim

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val errors = List(
        ("AA1234", None, None, None, None, BAD_REQUEST, NinoFormatError),
        ("AA123456A", Some("XXXX-YY"), None, None, None, BAD_REQUEST, TaxYearFormatError),
        ("AA123456A", Some("2018-19"), None, None, None, BAD_REQUEST, RuleTaxYearNotSupportedError),
        ("AA123456A", Some("2019-21"), None, None, None, BAD_REQUEST, RuleTaxYearRangeInvalidError),
        ("AA123456A", None, Some("employment"), None, None, BAD_REQUEST, TypeOfLossFormatError),
        ("AA123456A", None, Some("self-employment"), Some("XKIS0000000"), None, BAD_REQUEST, BusinessIdFormatError),
        ("AA123456A", None, None, None, Some("FORWARD"), BAD_REQUEST, TypeOfClaimFormatError)
      )

      errors.foreach(args => (validationErrorTest _).tupled(args))
    }

  }

}
