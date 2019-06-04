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

package v2.enums

import play.api.libs.json.{Format, Json}
import support.UnitSpec

class EnumJsonSpec extends UnitSpec {

  object TestEnum extends Enumeration {
    type TestEnum = Value

    val ITEM1, ITEM2 = Value

    implicit def formatAPIStatus: Format[TestEnum] = EnumJson.enumFormat(TestEnum)
  }

  val json = Json.parse(
    """
      |{
      |   "item" : "ITEM1"
      |}
    """.stripMargin)

  case class Holder(item: TestEnum.TestEnum)

  object Holder {
    implicit val format: Format[Holder] = Json.format[Holder]
  }

  "EnumJson" must {
    "allow json reads" in {
      json.as[Holder] shouldBe Holder(TestEnum.ITEM1)
    }

    "allow json writes" in {
      Json.toJson(Holder(TestEnum.ITEM1)) shouldBe json
    }
  }
}
