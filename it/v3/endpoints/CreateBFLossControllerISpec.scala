/*
 * Copyright 2021 HM Revenue & Customs
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

///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package v3.endpoints
//
//import com.github.tomakehurst.wiremock.stubbing.StubMapping
//import play.api.http.HeaderNames.ACCEPT
//import play.api.http.Status
//import play.api.libs.json.{JsObject, JsValue, Json}
//import play.api.libs.ws.{WSRequest, WSResponse}
//import support.V3IntegrationBaseSpec
//import v3.hateoas.HateoasLinks
//import v3.models.errors._
//import v3.stubs.{AuditStub, AuthStub, IfsStub, MtdIdLookupStub}
//
//class CreateBFLossControllerISpec extends V3IntegrationBaseSpec {
//
//  def generateBFLoss(businessId: Option[String], typeOfLoss: String, taxYear: String, lossAmount: BigDecimal): JsObject =
//    Json.obj("businessId" -> businessId, "typeOfLoss" -> typeOfLoss, "taxYear" -> taxYear, "lossAmount" -> lossAmount)
//
//  val lossId        = "AAZZ1234567890a"
//  val correlationId = "X-123"
//  val businessId    = "XKIS00000000988"
//  val taxYear       = "2019-20"
//  val lossAmount    = 256.78
//  val typeOfLoss    = "self-employment"
//
//  object Hateoas extends HateoasLinks
//
//  private trait Test {
//
//    val nino = "AA123456A"
//
//    val requestJson: JsValue = Json.parse("""
//        |{
//        |    "businessId": "XKIS00000000988",
//        |    "typeOfLoss": "self-employment",
//        |    "taxYear": "2019-20",
//        |    "lossAmount": 256.78
//        |}
//      """.stripMargin)
//
//    lazy val responseJson: JsValue = Json.parse(s"""
//        |{
//        |    "id": "AAZZ1234567890a",
//        |    "links": [{
//        |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
//        |      "method": "GET",
//        |      "rel": "self"
//        |    },
//        |    {
//        |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId",
//        |      "method": "DELETE",
//        |      "rel": "delete-brought-forward-loss"
//        |    },{
//        |      "href": "/individuals/losses/$nino/brought-forward-losses/$lossId/change-loss-amount",
//        |      "method": "POST",
//        |      "rel": "amend-brought-forward-loss"
//        |    }
//        |    ]
//        |}
//      """.stripMargin)
//
//    val downstreamResponseJson: JsValue = Json.parse("""
//        |{
//        |    "lossId": "AAZZ1234567890a"
//        |}
//      """.stripMargin)
//
//    def errorBody(code: String): String =
//      s"""
//         |      {
//         |        "code": "$code",
//         |        "reason": "downstream message"
//         |      }
//      """.stripMargin
//
//    def setupStubs(): StubMapping
//
//    def uri: String
//
//    def request(): WSRequest = {
//      setupStubs()
//      buildRequest(uri)
//        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.3.0+json"))
//    }
//
//  }
//
//  "Calling the create BFLoss endpoint" should {
//
//    trait CreateBFLossControllerTest extends Test {
//      def uri: String    = s"/$nino/brought-forward-losses"
//      def ifsUrl: String = s"/income-tax/brought-forward-losses/$nino"
//    }
//
//    "return a 201 status code" when {
//
//      "any valid request is made" in new CreateBFLossControllerTest() {
//
//        override def setupStubs(): StubMapping = {
//          AuditStub.audit()
//          AuthStub.authorised()
//          MtdIdLookupStub.ninoFound(nino)
//          IfsStub.onSuccess(IfsStub.POST, ifsUrl, Status.OK, downstreamResponseJson)
//        }
//
//        val response: WSResponse = await(request().post(requestJson))
//        response.status shouldBe Status.CREATED
//        response.json shouldBe responseJson
//        response.header("X-CorrelationId").nonEmpty shouldBe true
//      }
//    }
//
//    "return 500 (Internal Server Error)" when {
//
//      createErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.INTERNAL_SERVER_ERROR, DownstreamError)
//      createErrorTest(Status.BAD_REQUEST, "UNEXPECTED_IFS_ERROR_CODE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
//      createErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
//      createErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
//    }
//
//    "return 403 FORBIDDEN" when {
//      createErrorTest(Status.CONFLICT, "DUPLICATE", Status.FORBIDDEN, RuleDuplicateSubmissionError)
//    }
//
//    "return 404 NOT FOUND" when {
//      createErrorTest(Status.NOT_FOUND, "NOT_FOUND_INCOME_SOURCE", Status.NOT_FOUND, NotFoundError)
//    }
//
//    "return 400 (Bad Request)" when {
//
//      Seq("uk-property-fhl", "uk-property-non-fhl").foreach(
//        typeOfLoss =>
//          s"$typeOfLoss is supplied with a businessId" in new CreateBFLossControllerTest {
//            override def setupStubs(): StubMapping = {
//              AuditStub.audit()
//              AuthStub.authorised()
//              MtdIdLookupStub.ninoFound(nino)
//            }
//
//            override val requestJson: JsValue = Json.parse(s"""
//            |{
//            |    "businessId": "XKIS00000000988",
//            |    "typeOfLoss": "$typeOfLoss",
//            |    "taxYear": "2019-20",
//            |    "lossAmount": 256.78
//            |}
//      """.stripMargin)
//
//            val response: WSResponse = await(request().post(requestJson))
//            response.status shouldBe Status.BAD_REQUEST
//            response.json shouldBe Json.toJson(RuleBusinessId)
//            response.header("Content-Type") shouldBe Some("application/json")
//        }
//      )
//
//      Seq("self-employment", "self-employment-class4", "foreign-property-fhl-eea", "foreign-property").foreach(
//        typeOfLoss =>
//          s"$typeOfLoss is supplied without a businessId" in new CreateBFLossControllerTest {
//            override def setupStubs(): StubMapping = {
//              AuditStub.audit()
//              AuthStub.authorised()
//              MtdIdLookupStub.ninoFound(nino)
//            }
//
//            override val requestJson: JsValue = Json.parse(s"""
//            |{
//            |    "typeOfLoss": "$typeOfLoss",
//            |    "taxYear": "2019-20",
//            |    "lossAmount": 256.78
//            |}
//      """.stripMargin)
//
//            val response: WSResponse = await(request().post(requestJson))
//            response.status shouldBe Status.BAD_REQUEST
//            response.json shouldBe Json.toJson(RuleBusinessId)
//            response.header("Content-Type") shouldBe Some("application/json")
//        }
//      )
//
//      createErrorTest(Status.UNPROCESSABLE_ENTITY, "INCOMESOURCE_ID_REQUIRED", Status.BAD_REQUEST, RuleBusinessId)
//      createErrorTest(Status.FORBIDDEN, "TAX_YEAR_NOT_SUPPORTED", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
//      createErrorTest(Status.FORBIDDEN, "TAX_YEAR_NOT_ENDED", Status.BAD_REQUEST, RuleTaxYearNotEndedError)
//      createBFLossValidationErrorTest("BADNINO",
//                                      generateBFLoss(Some(businessId), typeOfLoss, taxYear, lossAmount),
//                                      Status.BAD_REQUEST,
//                                      NinoFormatError)
//      createBFLossValidationErrorTest("AA123456A",
//                                      generateBFLoss(Some(businessId), typeOfLoss, "20111", lossAmount),
//                                      Status.BAD_REQUEST,
//                                      TaxYearFormatError.copy(paths = Some(List("/taxYear"))))
//      createBFLossValidationErrorTest("AA123456A", Json.obj(), Status.BAD_REQUEST, RuleIncorrectOrEmptyBodyError)
//      createBFLossValidationErrorTest("AA123456A",
//                                      generateBFLoss(Some(businessId), typeOfLoss, "2011-12", lossAmount),
//                                      Status.BAD_REQUEST,
//                                      RuleTaxYearNotSupportedError)
//      createBFLossValidationErrorTest(
//        "AA123456A",
//        generateBFLoss(Some(businessId), typeOfLoss, "2019-25", lossAmount),
//        Status.BAD_REQUEST,
//        RuleTaxYearRangeInvalid.copy(paths = Some(List("/taxYear")))
//      )
//      createBFLossValidationErrorTest("AA123456A",
//                                      generateBFLoss(None, "self-employment-class", "2019-20", lossAmount),
//                                      Status.BAD_REQUEST,
//                                      TypeOfLossFormatError)
//      createBFLossValidationErrorTest("AA123456A",
//                                      generateBFLoss(Some("sdfsf"), typeOfLoss, "2019-20", lossAmount),
//                                      Status.BAD_REQUEST,
//                                      BusinessIdFormatError)
//      createBFLossValidationErrorTest("AA123456A",
//                                      generateBFLoss(Some(businessId), typeOfLoss, taxYear, -3234.99),
//                                      Status.BAD_REQUEST,
//                                      RuleInvalidLossAmount)
//      createBFLossValidationErrorTest("AA123456A",
//                                      generateBFLoss(Some(businessId), typeOfLoss, taxYear, 99999999999.999),
//                                      Status.BAD_REQUEST,
//                                      AmountFormatError)
//    }
//
//    def createErrorTest(ifsStatus: Int, ifsCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
//      s"downstream returns an $ifsCode error" in new CreateBFLossControllerTest {
//
//        override def setupStubs(): StubMapping = {
//          AuditStub.audit()
//          AuthStub.authorised()
//          MtdIdLookupStub.ninoFound(nino)
//          IfsStub.onError(IfsStub.POST, ifsUrl, ifsStatus, errorBody(ifsCode))
//        }
//
//        val response: WSResponse = await(request().post(requestJson))
//        response.status shouldBe expectedStatus
//        response.json shouldBe Json.toJson(expectedBody)
//        response.header("X-CorrelationId").nonEmpty shouldBe true
//        response.header("Content-Type") shouldBe Some("application/json")
//      }
//    }
//
//    def createBFLossValidationErrorTest(requestNino: String, requestBody: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
//      s"validation fails with ${expectedBody.code} error" in new CreateBFLossControllerTest {
//
//        override val nino: String = requestNino
//        override def setupStubs(): StubMapping = {
//          AuditStub.audit()
//          AuthStub.authorised()
//          MtdIdLookupStub.ninoFound(requestNino)
//        }
//
//        val response: WSResponse = await(request().post(requestBody))
//        response.status shouldBe expectedStatus
//        response.json shouldBe Json.toJson(expectedBody)
//        response.header("Content-Type") shouldBe Some("application/json")
//      }
//    }
//  }
//}
