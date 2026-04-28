/*
 * Copyright 2026 HM Revenue & Customs
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

package shared.definition

import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}
import shared.routing.Version3
import shared.utils.UnitSpec

class ApiDefinitionSpec extends UnitSpec {

  private val apiVersion: APIVersion       = APIVersion(Version3, APIStatus.ALPHA, endpointsEnabled = true)
  private val apiDefinition: APIDefinition = APIDefinition("b", "c", "d", List("category"), List(apiVersion), Some(false))

  "APIVersion" when {
    "created with valid parameters" should {
      "construct successfully with all fields" in {
        val version = APIVersion(Version3, APIStatus.BETA, endpointsEnabled = false)
        version.version shouldBe Version3
        version.status shouldBe APIStatus.BETA
        version.endpointsEnabled shouldBe false
      }
    }

    "created with different status values" should {
      "handle ALPHA status" in {
        val version = APIVersion(Version3, APIStatus.ALPHA, endpointsEnabled = true)
        version.status shouldBe APIStatus.ALPHA
      }

      "handle STABLE status" in {
        val version = APIVersion(Version3, APIStatus.STABLE, endpointsEnabled = true)
        version.status shouldBe APIStatus.STABLE
      }

      "handle DEPRECATED status" in {
        val version = APIVersion(Version3, APIStatus.DEPRECATED, endpointsEnabled = false)
        version.status shouldBe APIStatus.DEPRECATED
      }

      "handle RETIRED status" in {
        val version = APIVersion(Version3, APIStatus.RETIRED, endpointsEnabled = false)
        version.status shouldBe APIStatus.RETIRED
      }
    }

    "serialized to JSON" should {
      "produce correct JSON format" in {
        val json = Json.toJson(apiVersion)
        (json \ "status").as[String] shouldBe "ALPHA"
        (json \ "endpointsEnabled").as[Boolean] shouldBe true
      }
    }

    "deserialized from JSON" should {
      "reconstruct object correctly" in {
        val json    = Json.parse("""{"version":"3.0","status":"BETA","endpointsEnabled":false}""")
        val version = json.as[APIVersion]
        version.status shouldBe APIStatus.BETA
        version.endpointsEnabled shouldBe false
      }
    }

    "round-tripped through JSON" should {
      "maintain equality" in {
        val original     = APIVersion(Version3, APIStatus.STABLE, endpointsEnabled = true)
        val json         = Json.toJson(original)
        val deserialized = json.as[APIVersion]
        deserialized shouldBe original
      }
    }
  }

  "APIDefinition" when {
    "created with valid parameters" should {
      "construct successfully" in {
        val definition = APIDefinition("Test API", "A test API", "/test", List("TEST_CATEGORY"), List(apiVersion), None)
        definition.name shouldBe "Test API"
        definition.description shouldBe "A test API"
        definition.context shouldBe "/test"
        definition.categories should contain("TEST_CATEGORY")
        definition.versions should contain(apiVersion)
        definition.requiresTrust shouldBe None
      }
    }

    "the 'name' parameter is empty" should {
      "throw an 'IllegalArgumentException'" in {
        assertThrows[IllegalArgumentException](
          apiDefinition.copy(name = "")
        )
      }
    }

    "the 'description' parameter is empty" should {
      "throw an 'IllegalArgumentException'" in {
        assertThrows[IllegalArgumentException](
          apiDefinition.copy(description = "")
        )
      }
    }

    "the 'context' parameter is empty" should {
      "throw an 'IllegalArgumentException'" in {
        assertThrows[IllegalArgumentException](
          apiDefinition.copy(context = "")
        )
      }
    }

    "the 'categories' parameter is empty" should {
      "throw an 'IllegalArgumentException'" in {
        assertThrows[IllegalArgumentException](
          apiDefinition.copy(categories = Nil)
        )
      }
    }

    "the 'versions' parameter is empty" should {
      "throw an 'IllegalArgumentException'" in {
        assertThrows[IllegalArgumentException](
          apiDefinition.copy(versions = Nil)
        )
      }
    }

    "the 'versions' parameter is not unique" should {
      "throw an 'IllegalArgumentException'" in {
        assertThrows[IllegalArgumentException](
          apiDefinition.copy(versions = List(apiVersion, apiVersion))
        )
      }
    }

    "versions contain duplicate version numbers" should {
      "throw an 'IllegalArgumentException'" in {
        val versionA = APIVersion(Version3, APIStatus.ALPHA, endpointsEnabled = true)
        val versionB = APIVersion(Version3, APIStatus.BETA, endpointsEnabled = false)
        assertThrows[IllegalArgumentException](
          apiDefinition.copy(versions = List(versionA, versionB))
        )
      }
    }

    "with multiple categories" should {
      "construct successfully and maintain all categories" in {
        val categories = List("CAT1", "CAT2", "CAT3")
        val definition = apiDefinition.copy(categories = categories)
        definition.categories should contain allElementsOf categories
      }
    }

    "with requiresTrust as true" should {
      "construct successfully" in {
        val definition = apiDefinition.copy(requiresTrust = Some(true))
        definition.requiresTrust shouldBe Some(true)
      }
    }

    "with requiresTrust as false" should {
      "construct successfully" in {
        val definition = apiDefinition.copy(requiresTrust = Some(false))
        definition.requiresTrust shouldBe Some(false)
      }
    }

    "with requiresTrust as None" should {
      "construct successfully" in {
        val definition = apiDefinition.copy(requiresTrust = None)
        definition.requiresTrust shouldBe None
      }
    }

    "serialized to JSON" should {
      "produce correct JSON format" in {
        val definition = APIDefinition("Test API", "Description", "/context", List("CAT1"), List(apiVersion), Some(true))
        val json       = Json.toJson(definition)
        (json \ "name").as[String] shouldBe "Test API"
        (json \ "description").as[String] shouldBe "Description"
        (json \ "context").as[String] shouldBe "/context"
        (json \ "categories").as[List[String]] should contain("CAT1")
      }
    }

    "deserialized from JSON" should {
      "reconstruct the object correctly" in {
        val json = Json.parse("""
            {
              "name": "Test API",
              "description": "A test description",
              "context": "/test",
              "categories": ["CATEGORY1", "CATEGORY2"],
              "versions": [{"version":"3.0","status":"ALPHA","endpointsEnabled":true}],
              "requiresTrust": false
            }
          """)
        val definition = json.as[APIDefinition]
        definition.name shouldBe "Test API"
        definition.description shouldBe "A test description"
        definition.context shouldBe "/test"
        definition.categories should contain("CATEGORY1")
        definition.categories should contain("CATEGORY2")
        definition.requiresTrust shouldBe Some(false)
      }
    }

    "round-tripped through JSON" should {
      "maintain equality" in {
        val original     = APIDefinition("API Name", "Description", "/api", List("CAT1", "CAT2"), List(apiVersion), None)
        val json         = Json.toJson(original)
        val deserialized = json.as[APIDefinition]
        deserialized shouldBe original
      }
    }
  }

  "Definition" when {
    "created with a valid APIDefinition" should {
      "construct successfully" in {
        val definition = Definition(apiDefinition)
        definition.api shouldBe apiDefinition
      }
    }

    "created with different APIDefinition instances" should {
      "maintain reference to the provided APIDefinition" in {
        val customDef  = APIDefinition("Custom", "Custom API", "/custom", List("CUSTOM"), List(apiVersion), Some(true))
        val definition = Definition(customDef)
        definition.api.name shouldBe "Custom"
        definition.api.description shouldBe "Custom API"
      }
    }

    "serialized to JSON" should {
      "produce correct JSON format with nested api field" in {
        val definition = Definition(apiDefinition)
        val json       = Json.toJson(definition)
        (json \ "api" \ "name").as[String] shouldBe "b"
        (json \ "api" \ "description").as[String] shouldBe "c"
        (json \ "api" \ "context").as[String] shouldBe "d"
      }
    }

    "deserialized from JSON" should {
      "reconstruct the object correctly with nested structure" in {
        val json = Json.parse("""
            {
              "api": {
                "name": "API Name",
                "description": "API Description",
                "context": "/context",
                "categories": ["CAT"],
                "versions": [{"version":"3.0","status":"ALPHA","endpointsEnabled":true}],
                "requiresTrust": null
              }
            }
          """)
        val definition = json.as[Definition]
        definition.api.name shouldBe "API Name"
        definition.api.description shouldBe "API Description"
        definition.api.context shouldBe "/context"
      }
    }

    "round-tripped through JSON" should {
      "maintain equality after serialization and deserialization" in {
        val original     = Definition(apiDefinition)
        val json         = Json.toJson(original)
        val deserialized = json.as[Definition]
        deserialized shouldBe original
      }
    }

  }

  "Json formats" when {
    "APIVersion format" should {
      "be accessible and work correctly" in {
        val format   = summon[OFormat[APIVersion]]
        val version  = APIVersion(Version3, APIStatus.BETA, endpointsEnabled = false)
        val json     = format.writes(version)
        val readBack = format.reads(json)
        readBack shouldBe JsSuccess(version)
      }

      "handle invalid JSON gracefully" in {
        val format      = summon[OFormat[APIVersion]]
        val invalidJson = Json.obj("invalid" -> "data")
        val result      = format.reads(invalidJson)
        result shouldBe a[JsError]
      }

      "handle missing required fields" in {
        val format         = summon[OFormat[APIVersion]]
        val incompleteJson = Json.obj("version" -> "3.0")
        val result         = format.reads(incompleteJson)
        result shouldBe a[JsError]
      }
    }

    "APIDefinition format" should {
      "be accessible and work correctly" in {
        val format     = summon[OFormat[APIDefinition]]
        val definition = APIDefinition("Test", "Test API", "/test", List("CAT"), List(apiVersion), None)
        val json       = format.writes(definition)
        val readBack   = format.reads(json)
        readBack shouldBe JsSuccess(definition)
      }

      "handle invalid JSON gracefully" in {
        val format      = summon[OFormat[APIDefinition]]
        val invalidJson = Json.obj("invalid" -> "data")
        val result      = format.reads(invalidJson)
        result shouldBe a[JsError]
      }

      "handle missing required fields" in {
        val format         = summon[OFormat[APIDefinition]]
        val incompleteJson = Json.obj("name" -> "Test")
        val result         = format.reads(incompleteJson)
        result shouldBe a[JsError]
      }
    }

    "Definition format" should {
      "be accessible and work correctly" in {
        val format     = summon[OFormat[Definition]]
        val definition = Definition(apiDefinition)
        val json       = format.writes(definition)
        val readBack   = format.reads(json)
        readBack shouldBe JsSuccess(definition)
      }

      "handle invalid JSON gracefully" in {
        val format      = summon[OFormat[Definition]]
        val invalidJson = Json.obj("invalid" -> "data")
        val result      = format.reads(invalidJson)
        result shouldBe a[JsError]
      }

      "handle missing required fields" in {
        val format         = summon[OFormat[Definition]]
        val incompleteJson = Json.obj()
        val result         = format.reads(incompleteJson)
        result shouldBe a[JsError]
      }
    }
  }

}
