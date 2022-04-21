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

package config

import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import support.IntegrationBaseSpec

class DocumentationISpec extends IntegrationBaseSpec {

  val apiDefinitionJson: JsValue = Json.parse("""
      |{
      |  "scopes":[
      |    {
      |      "key":"read:self-assessment",
      |      "name":"View your Self Assessment information",
      |      "description":"Allow read access to self assessment data",
      |      "confidenceLevel": 200
      |    },
      |    {
      |      "key":"write:self-assessment",
      |      "name":"Change your Self Assessment information",
      |      "description":"Allow write access to self assessment data",
      |      "confidenceLevel": 200
      |    }
      |  ],
      |  "api":{
      |    "name":"Individual Losses (MTD)",
      |    "description":"An API for providing individual losses data",
      |    "context":"individuals/losses",
      |    "versions":[
      |      {
      |        "version":"2.0",
      |        "status":"RETIRED",
      |        "endpointsEnabled":true
      |      },
      |      {
      |        "version":"3.0",
      |        "status":"ALPHA",
      |        "endpointsEnabled":true
      |      }
      |    ]
      |  }
      |}
    """.stripMargin)

  "GET /api/definition" should {
    "return a 200 with the correct response body" in {

      val response: WSResponse = await(buildRequest("/api/definition").get())
      Json.parse(response.body) shouldBe apiDefinitionJson
      response.status shouldBe Status.OK
    }
  }

  "a documentation request" must {
    "return no v1 documentation" in {
      val response: WSResponse = await(buildRequest("/api/conf/1.0/application.raml").get())
      response.status shouldBe Status.NOT_FOUND
    }
    "return the v2 documentation" in {
      val response: WSResponse = await(buildRequest("/api/conf/2.0/application.raml").get())
      response.status shouldBe Status.OK
      response.body[String] should startWith("#%RAML 1.0")
    }
    "return the v3 documentation" in {
      val response: WSResponse = await(buildRequest("/api/conf/3.0/application.raml").get())
      response.status shouldBe Status.OK
      response.body[String] should startWith("#%RAML 1.0")
    }
  }

}
