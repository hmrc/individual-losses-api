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

package api.endpoints.bfLoss.list.v3

import v3.fixtures.ListBFLossesFixtures._
import api.models.domain.TaxYear
import api.models.errors._
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import support.V3V4IntegrationBaseSpec
import support.stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}

class ListBFLossesControllerISpec extends V3V4IntegrationBaseSpec {

  private trait Test {

    val nino: String               = "AA123456A"
    val typeOfLoss: Option[String] = None
    val businessId: Option[String] = None

    def taxYearBroughtForwardFrom: Option[String] = Some("2019-20")

    def mtdUri: String = s"/$nino/brought-forward-losses"

    def mtdQueryParams: Seq[(String, String)] =
      Seq("taxYearBroughtForwardFrom" -> taxYearBroughtForwardFrom, "typeOfLoss" -> typeOfLoss, "businessId" -> businessId)
        .collect { case (k, Some(v)) =>
          (k, v)
        }

    def downstreamUrl(taxYear: String = "2019-20"): String = s"/income-tax/brought-forward-losses/${TaxYear.fromMtd(taxYear).asTysDownstream}/$nino"

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "downstream message"
         |      }
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

      buildRequest(mtdUri)
        .addQueryStringParameters(mtdQueryParams: _*)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.3.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

  }

  "Calling the ListBFLosses endpoint" should {

    "return a 200 status code" when {

      "query with a tax year" in new Test {

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |    "losses": [${mtdLossJsonWithSelfEmployment(nino, "2019-20")}],
             |    "links": ${baseHateoasLinks(nino)}
             |}
             |""".stripMargin
        )

        override def setupStubs(): Unit = {
          stubDownstream(downstreamResponseJsonWithLossType())
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "query without a tax year" in new Test {
        override val taxYearBroughtForwardFrom: Option[String] = None

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |    "losses": [
             |        ${mtdLossJsonWithSelfEmployment(nino, "2019-20")},
             |        ${mtdLossJsonWithSelfEmployment(nino, "2020-21")},
             |        ${mtdLossJsonWithSelfEmployment(nino, "2021-22")},
             |        ${mtdLossJsonWithSelfEmployment(nino, "2022-23")}
             |    ],
             |    "links": ${baseHateoasLinks(nino)}
             |}
             |""".stripMargin
        )

        override def setupStubs(): Unit = {
          stubDownstream(downstreamResponseJsonWithLossType("2019-20"), "2019-20")
          stubDownstream(downstreamResponseJsonWithLossType("2020-21"), "2020-21")
          stubDownstream(downstreamResponseJsonWithLossType("2021-22"), "2021-22")
          stubDownstream(downstreamResponseJsonWithLossType("2022-23"), "2022-23")
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "query without a tax year and some responses are 404 NOT_FOUND" in new Test {
        override val taxYearBroughtForwardFrom: Option[String] = None

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |    "losses": [
             |        ${mtdLossJsonWithSelfEmployment(nino, "2019-20")},
             |        ${mtdLossJsonWithSelfEmployment(nino, "2021-22")}
             |    ],
             |    "links": ${baseHateoasLinks(nino)}
             |}
             |""".stripMargin
        )

        override def setupStubs(): Unit = {
          stubDownstream(downstreamResponseJsonWithLossType("2019-20"), "2019-20")
          DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2020-21"), Map.empty, NOT_FOUND, errorBody("NOT_FOUND"))
          stubDownstream(downstreamResponseJsonWithLossType("2021-22"), "2021-22")
          DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2022-23"), Map.empty, NOT_FOUND, errorBody("NOT_FOUND"))
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying with specific typeOfLoss = uk-property-fhl" in new Test {
        override val typeOfLoss: Option[String] = Some("uk-property-fhl")
        override val businessId: Option[String] = None

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |    "losses": [
             |        ${mtdLossJsonWithUkPropertyFhl(nino, "2019-20")}
             |    ],
             |    "links": ${baseHateoasLinks(nino)}
             |}
             |""".stripMargin
        )

        override def setupStubs(): Unit = {
          stubDownstream(downstreamResponseJsonWithIncomeSourceType(), params = Map("incomeSourceType" -> "04"))
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying with specific typeOfLoss = self-employment" in new Test {
        override val typeOfLoss: Option[String] = Some("self-employment")
        override val businessId: Option[String] = None

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |    "losses": [
             |        ${mtdLossJsonWithSelfEmployment(nino, "2019-20")}
             |    ],
             |    "links": ${baseHateoasLinks(nino)}
             |}
             |""".stripMargin
        )

        override def setupStubs(): Unit = {
          stubDownstream(downstreamResponseJsonWithLossType(), params = Map("incomeSourceType" -> "01"))
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "querying with businessId, taxYear and typeOfLoss" in new Test {
        override def taxYearBroughtForwardFrom: Option[String] = Some("2019-20")
        override val typeOfLoss: Option[String]                = Some("uk-property-fhl")
        override val businessId: Option[String]                = Some("XKIS00000000988")

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |    "losses": [
             |        ${mtdLossJsonWithUkPropertyFhl(nino, "2019-20")}
             |    ],
             |    "links": ${baseHateoasLinks(nino)}
             |}
             |""".stripMargin
        )

        override def setupStubs(): Unit = {
          stubDownstream(
            response = downstreamResponseJsonWithIncomeSourceType(),
            params = Map("incomeSourceId" -> "XKIS00000000988", "incomeSourceType" -> "04")
          )
        }

        val response: WSResponse = await(request().get())
        response.json shouldBe responseJson
        response.status shouldBe OK
        response.header("X-CorrelationId").nonEmpty shouldBe true
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
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val errors = List(
        (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
        (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError),
        (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
        (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
        (BAD_REQUEST, "INVALID_INCOMESOURCE_ID", BAD_REQUEST, BusinessIdFormatError),
        (BAD_REQUEST, "INVALID_INCOMESOURCE_TYPE", BAD_REQUEST, TypeOfLossFormatError),
        (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
      )

      errors.foreach(args => (serviceErrorTest _).tupled(args))
    }

    "handle downstream errors according to spec without a tax year" when {
      def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns an $downstreamCode error" in new Test {
          override val taxYearBroughtForwardFrom: Option[String] = None

          override def setupStubs(): Unit = {
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2019-20"), Map.empty, downstreamStatus, errorBody(downstreamCode))
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2020-21"), Map.empty, downstreamStatus, errorBody(downstreamCode))
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2021-22"), Map.empty, downstreamStatus, errorBody(downstreamCode))
            DownstreamStub.onError(DownstreamStub.GET, downstreamUrl("2022-23"), Map.empty, downstreamStatus, errorBody(downstreamCode))
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("X-CorrelationId").nonEmpty shouldBe true
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val errors = List(
        (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
        (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError),
        (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
        (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_TAX_YEAR", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_INCOMESOURCE_ID", BAD_REQUEST, BusinessIdFormatError),
        (BAD_REQUEST, "INVALID_INCOMESOURCE_TYPE", BAD_REQUEST, TypeOfLossFormatError),
        (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", INTERNAL_SERVER_ERROR, InternalError)
      )

      errors.foreach(args => (serviceErrorTest _).tupled(args))
    }

    "handle validation errors according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: Option[String],
                              requestTypeOfLoss: Option[String],
                              requestBusinessId: Option[String],
                              expectedStatus: Int,
                              expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {

          override val nino: String                              = requestNino
          override val taxYearBroughtForwardFrom: Option[String] = requestTaxYear
          override val typeOfLoss: Option[String]                = requestTypeOfLoss
          override val businessId: Option[String]                = requestBusinessId

          val response: WSResponse = await(request().get())
          response.status shouldBe expectedStatus
          response.json shouldBe Json.toJson(expectedBody)
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val errors = List(
        ("BADNINO", None, None, None, BAD_REQUEST, NinoFormatError),
        ("AA123456A", Some("XXXX-YY"), None, None, BAD_REQUEST, TaxYearFormatError),
        ("AA123456A", Some("2017-18"), None, None, BAD_REQUEST, RuleTaxYearNotSupportedError),
        ("AA123456A", Some("2019-21"), None, None, BAD_REQUEST, RuleTaxYearRangeInvalidError),
        ("AA123456A", None, Some("bad-loss-type"), None, BAD_REQUEST, TypeOfLossFormatError),
        ("AA123456A", None, Some("self-employment"), Some("bad-self-employment-id"), BAD_REQUEST, BusinessIdFormatError)
      )

      errors.foreach(args => (validationErrorTest _).tupled(args))
    }

  }

}
