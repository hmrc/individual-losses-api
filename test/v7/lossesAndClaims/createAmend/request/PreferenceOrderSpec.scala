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

package v7.lossesAndClaims.createAmend.request

import org.scalactic.Prettifier.default
import play.api.libs.json.{JsValue, Json}
import shared.utils.UnitSpec

class PreferenceOrderSpec extends UnitSpec {

  val requestBody: PreferenceOrder = PreferenceOrder(
    Option("carry-back")
  )

  val defaultMtdRequestJson: JsValue = Json.parse(s"""
       |{
       |      "applyFirst": "carry-back"
       |}
       |""".stripMargin)

  "Json Validate" should {
    "read a default model from JSON" in {
      val result = defaultMtdRequestJson.validate[PreferenceOrder]
      result.isSuccess shouldBe true
    }
  }

}
