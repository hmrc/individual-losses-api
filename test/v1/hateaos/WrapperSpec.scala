/*
 * Copyright 2019 HM Revenue & Customs
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
package v1.hateaos

import play.api.libs.json.{Json, OWrites}
import support.UnitSpec

class WrapperSpec extends UnitSpec {

  case class TestMtdResponse(field1: String, field2: Int)

  object TestMtdResponse {
    implicit val writes: OWrites[TestMtdResponse] = Json.writes[TestMtdResponse]
  }

  "wrapper writes" must {
    "place links alongside wrapped object fields" in {
      Json.toJson(Wrapper(TestMtdResponse("value1", 123), Seq(Link("/some/resource", "GET", "thing")))) shouldBe
        Json.parse("""
      |{
      |"field1": "value1",
      |"field2": 123,
      |"links" : [
      |  {
      |    "href": "/some/resource",
      |    "rel": "thing",
      |    "method": "GET"
      |  }
      |]}
      |
    """.stripMargin)
    }
  }
}
