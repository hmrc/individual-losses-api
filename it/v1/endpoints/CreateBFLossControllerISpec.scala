/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class CreateBFLossControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino    =  "AA123456A"
    val lossId  = "AAZZ1234567890a"
    val correlationId = "X-123"

    val requestBody: JsValue = Json.parse(
      """
        |{
        |    "selfEmploymentId": "XKIS00000000988",
        |    "typeOfLoss": "self-employment",
        |    "taxYear": "2019-20",
        |    "lossAmount": 256.78
        |}
      """.stripMargin)

    def setupStubs(): StubMapping

    def uri: String

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "des message"
         |      }
      """.stripMargin
  }

  "Calling the create BFLoss endpoint" should {

    trait CreateBFLossControllerTest extends Test {
      def uri: String = s"/individual/losses/$nino/brought-forward-losses"
    }

    "return a 201 status code" when {

      "any valid request is made" in new CreateBFLossControllerTest() {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.serviceSuccess(nino)
        }

        val response: WSResponse = await(request().post(requestBody))
        response.status shouldBe Status.CREATED
      }
    }
  }
}
