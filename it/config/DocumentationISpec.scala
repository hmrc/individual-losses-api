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

package config

import play.api.http.Status
import play.api.libs.json.{ JsValue, Json }
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
      |        "version":"3.0",
      |        "status":"ALPHA",
      |        "endpointsEnabled":true
      |      },
      |      {
      |        "version":"4.0",
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

  "a RAML documentation request" must {
    "return no v1 documentation" in {
      val response: WSResponse = await(buildRequest("/api/conf/1.0/application.raml").get())
      response.status shouldBe Status.NOT_FOUND
    }
    "return no v2 documentation" in {
      val response: WSResponse = await(buildRequest("/api/conf/2.0/application.raml").get())
      response.status shouldBe Status.NOT_FOUND
    }
    "return the v3 documentation" in {
      val response: WSResponse = await(buildRequest("/api/conf/3.0/application.raml").get())
      response.status shouldBe Status.OK
      response.body[String] should startWith("#%RAML 1.0")
    }
    "return the v4 documentation" in {
      val response: WSResponse = await(buildRequest("/api/conf/4.0/application.raml").get())
      response.status shouldBe Status.OK
      response.body[String] should startWith("#%RAML 1.0")
    }
  }

  "an OAS documentation request" must {
//    "return the V3 documentation that passes OAS V3 parser" in {
//      val response: WSResponse = await(buildRequest("/api/conf/3.0/application.yaml").get())
//      response.status shouldBe Status.OK
//
//      val contents     = response.body[String]
//      val parserResult = Try(new OpenAPIV3Parser().readContents(contents))
//      parserResult.isSuccess shouldBe true
//
//      val openAPI = Option(parserResult.get.getOpenAPI)
//      openAPI.isEmpty shouldBe false
//      openAPI.get.getOpenapi shouldBe "3.0.3"
//      openAPI.get.getInfo.getTitle shouldBe "Individual Losses (MTD)"
//      openAPI.get.getInfo.getVersion shouldBe "3.0"
//    }
//
//    "return the V4 documentation that passes OAS V3 parser" in {
//      val response: WSResponse = await(buildRequest("/api/conf/4.0/application.yaml").get())
//      response.status shouldBe Status.OK
//
//      val contents     = response.body[String]
//      val parserResult = Try(new OpenAPIV3Parser().readContents(contents))
//      parserResult.isSuccess shouldBe true
//
//      val openAPI = Option(parserResult.get.getOpenAPI)
//      openAPI.isEmpty shouldBe false
//      openAPI.get.getOpenapi shouldBe "3.0.3"
//      openAPI.get.getInfo.getTitle shouldBe "Individual Losses (MTD)"
//      openAPI.get.getInfo.getVersion shouldBe "4.0"
//    }
  }

}
