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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.controllers.requestParsers.validators.validations

import play.api.libs.json.Json
import support.UnitSpec
import v1.models.errors.NinoFormatError

class JsonValidationSpec extends UnitSpec {
  private def unusedValidation[T] = (_: T) => throw new RuntimeException

  "JsonValidation" when {
    val json = Json.parse("""
            |{
            | "field1": "value1"
            |}
          """.stripMargin)

    "the path element does not exist" must {
      "return empty" in {
        JsonValidation.validate[String](json \ "nosuchfield")(unusedValidation) shouldBe Nil
      }
    }

    "the path element is the incorrect type for the Reads" must {
      "return empty" in {
        JsonValidation.validate[Int](json \ "field1")(unusedValidation) shouldBe Nil
      }
    }

    "the path element is readable by the Reads" when {
      "the value is valid" must {
        "return empty" in {
          JsonValidation.validate[String](json \ "field1")((_: String) => Nil)  shouldBe Nil
        }
      }

      "the value is invalid" must {
        "return the value of the validation" in {
          JsonValidation.validate[String](json \ "field1")((_: String) => List(NinoFormatError)) shouldBe List(NinoFormatError)
        }
      }
    }
  }
}
